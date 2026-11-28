package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Live Valhalla HTTP PathRouter (ATNA-113).
 *
 * Interim: hosted Valhalla (e.g. Simple Routing `https://api.simplerouting.io/valhalla`)
 * with optional Bearer [apiKey]. Long-term: same client against self-hosted /
 * on-device Valhalla tiles — swap [baseUrl], clear key.
 *
 * Truck costing from [GraphHopperCustomModelStub] / vehicle profile.
 * Shape is Valhalla encoded polyline **precision 6**.
 */
class LiveValhallaPathRouter(
    private val baseUrl: String,
    private val apiKey: String? = null,
    private val httpClient: (url: String, body: String, bearerToken: String?) -> String =
        ::defaultHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PathRouter {

    override suspend fun path(
        start: RouteEndpoint,
        end: RouteEndpoint,
        preferences: TruckRoutingPreferences,
        customModel: GraphHopperCustomModelStub?,
        avoidUnderBridges: List<RoutingBridgeConstraint>,
    ): PathResult = withContext(ioDispatcher) {
        val model = customModel ?: GraphHopperCustomModelStub.default(preferences = preferences)
        val body = buildRequestBody(start, end, model, avoidUnderBridges)
        val url = baseUrl.trimEnd('/') + "/route"
        val raw = try {
            httpClient(url, body, apiKey?.trim()?.ifEmpty { null })
        } catch (e: Exception) {
            return@withContext PathResult.Blocked(reason = "Valhalla request failed: ${e.message}")
        }
        parseResponse(raw, preferences, model.toRequestSnippet())
    }

    private fun buildRequestBody(
        start: RouteEndpoint,
        end: RouteEndpoint,
        model: GraphHopperCustomModelStub,
        avoidUnderBridges: List<RoutingBridgeConstraint>,
    ): String {
        val exclude = excludePolygonsJson(avoidUnderBridges)
        val excludeBlock = if (exclude == "[]") {
            ""
        } else {
            """,
          "exclude_polygons": $exclude"""
        }
        return """
        {
          "locations": [
            {"lat": ${start.latitude}, "lon": ${start.longitude}},
            {"lat": ${end.latitude}, "lon": ${end.longitude}}
          ],
          "costing": "${ValhallaTruckCosting.COSTING}",
          "directions_options": { "units": "kilometers" },
          "costing_options": {
            "truck": ${ValhallaTruckCosting.optionsJson(model)}
          }$excludeBlock
        }
        """.trimIndent()
    }

    private fun excludePolygonsJson(avoid: List<RoutingBridgeConstraint>): String {
        if (avoid.isEmpty()) return "[]"
        val d = BridgePassagePolicy.NEAR_PATH_M / 111_320.0
        val polys = avoid.joinToString(", ") { bridge ->
            val south = bridge.latitude - d
            val north = bridge.latitude + d
            val west = bridge.longitude - d
            val east = bridge.longitude + d
            // Valhalla rings are [lon, lat]
            """[[$west, $south], [$east, $south], [$east, $north], [$west, $north], [$west, $south]]"""
        }
        return "[ $polys ]"
    }

    private fun parseResponse(
        raw: String,
        preferences: TruckRoutingPreferences,
        customModelSnippet: String,
    ): PathResult {
        if (raw.contains("\"error\"") && !raw.contains("\"trip\"")) {
            val msg = Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1)
            return PathResult.Blocked(reason = msg ?: "Valhalla returned an error")
        }
        if (!raw.contains("\"trip\"")) {
            return PathResult.Blocked(reason = "Valhalla response was not a trip JSON")
        }
        val points = extractShapePoints(raw)
        if (points.size < 2) {
            return PathResult.Blocked(reason = "Valhalla path needs at least two coordinates")
        }
        val timeSec = extractTripTimeSeconds(raw)
        val durationSeconds = if (timeSec > 0.0) {
            timeSec.roundToLong().coerceAtLeast(1L)
        } else {
            (points.size - 1).coerceAtLeast(1) * 60L
        }
        return PathResult.Ok(
            RoutePath(
                id = "valhalla-${points.first().latitude},${points.first().longitude}" +
                    "->${points.last().latitude},${points.last().longitude}",
                points = points,
                durationSeconds = durationSeconds,
                source = RoutePath.SOURCE_VALHALLA,
                preferences = preferences,
                customModelSnippet = customModelSnippet,
            ),
        )
    }

    internal fun extractShapePoints(raw: String): List<LatLngPoint> {
        val shapes = Regex(""""shape"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .findAll(raw)
            .map { it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") }
            .toList()
        if (shapes.isEmpty()) return emptyList()
        val out = ArrayList<LatLngPoint>()
        for (shape in shapes) {
            val decoded = EncodedPolylineDecoder.decode(shape, precision = 6)
            if (out.isEmpty()) {
                out += decoded
            } else if (decoded.isNotEmpty()) {
                // Skip duplicate join vertex between legs.
                out += decoded.drop(1)
            }
        }
        return out
    }

    private fun extractTripTimeSeconds(raw: String): Double {
        // Prefer trip.summary.time (seconds).
        val summary = Regex(""""summary"\s*:\s*\{[^}]*"time"\s*:\s*([0-9.]+)""")
            .findAll(raw)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .toList()
        return summary.maxOrNull() ?: 0.0
    }

    companion object {
        fun defaultHttpClient(url: String, body: String, bearerToken: String?): String {
            val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (!bearerToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                }
            }
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
