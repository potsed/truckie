package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import kotlin.math.roundToLong

/**
 * Live GraphHopper HTTP PathRouter (ATNA-9 fold-in for optimisation / Navigate).
 *
 * Expects a GraphHopper-compatible `/route` JSON response with `paths[].points`
 * GeoJSON coordinates `[lon, lat]` and `time` in milliseconds.
 *
 * Transport is injectable for unit tests. Production uses [defaultHttpClient].
 * Parsing stays dependency-free (no org.json) so `:core-routing` remains a plain JVM module.
 */
class LiveGraphHopperPathRouter(
    private val baseUrl: String,
    private val httpClient: (url: String, body: String) -> String = ::defaultHttpClient,
) : PathRouter {

    override fun path(
        start: RouteEndpoint,
        end: RouteEndpoint,
        preferences: TruckRoutingPreferences,
        customModel: GraphHopperCustomModelStub?,
    ): PathResult {
        val model = customModel ?: GraphHopperCustomModelStub.default(preferences = preferences)
        val body = buildRequestBody(start, end, model)
        val url = baseUrl.trimEnd('/') + "/route"
        val raw = try {
            httpClient(url, body)
        } catch (e: Exception) {
            return PathResult.Blocked(reason = "GraphHopper request failed: ${e.message}")
        }
        return parseResponse(raw, preferences, model.toRequestSnippet())
    }

    private fun buildRequestBody(
        start: RouteEndpoint,
        end: RouteEndpoint,
        model: GraphHopperCustomModelStub,
    ): String = """
        {
          "profile": "truck",
          "points": [
            [${start.longitude}, ${start.latitude}],
            [${end.longitude}, ${end.latitude}]
          ],
          "points_encoded": false,
          "instructions": false,
          "custom_model": ${model.toRequestSnippet()}
        }
    """.trimIndent()

    private fun parseResponse(
        raw: String,
        preferences: TruckRoutingPreferences,
        customModelSnippet: String,
    ): PathResult {
        if (!raw.contains("\"paths\"")) {
            return PathResult.Blocked(reason = "GraphHopper response was not JSON")
        }
        val coordinates = extractCoordinatePairs(raw)
        if (coordinates.isEmpty()) {
            return PathResult.Blocked(reason = "GraphHopper returned no paths")
        }
        if (coordinates.size < 2) {
            return PathResult.Blocked(reason = "GraphHopper path needs at least two coordinates")
        }
        val points = coordinates.map { (lon, lat) ->
            LatLngPoint(latitude = lat, longitude = lon)
        }
        val timeMs = extractTimeMs(raw)
        val durationSeconds = if (timeMs > 0L) {
            (timeMs / 1000.0).roundToLong().coerceAtLeast(1L)
        } else {
            (points.size - 1).coerceAtLeast(1) * 60L
        }
        return PathResult.Ok(
            RoutePath(
                id = "live-${points.first().latitude},${points.first().longitude}" +
                    "->${points.last().latitude},${points.last().longitude}",
                points = points,
                durationSeconds = durationSeconds,
                source = RoutePath.SOURCE_LIVE,
                preferences = preferences,
                customModelSnippet = customModelSnippet,
            ),
        )
    }

    /**
     * Pulls the first `"coordinates": [ [lon,lat], ... ]` array from a GH response.
     * GeoJSON order is lon, lat.
     */
    internal fun extractCoordinatePairs(raw: String): List<Pair<Double, Double>> {
        val key = "\"coordinates\""
        val keyIndex = raw.indexOf(key)
        if (keyIndex < 0) return emptyList()
        val arrayStart = raw.indexOf('[', keyIndex + key.length)
        if (arrayStart < 0) return emptyList()
        val arrayBody = sliceBalancedArray(raw, arrayStart) ?: return emptyList()
        val pairRegex = Regex("""\[\s*([+-]?\d+(?:\.\d+)?)\s*,\s*([+-]?\d+(?:\.\d+)?)\s*]""")
        return pairRegex.findAll(arrayBody).map { match ->
            val lon = match.groupValues[1].toDouble()
            val lat = match.groupValues[2].toDouble()
            lon to lat
        }.toList()
    }

    private fun extractTimeMs(raw: String): Long {
        val match = Regex(""""time"\s*:\s*(\d+)""").find(raw) ?: return 0L
        return match.groupValues[1].toLongOrNull() ?: 0L
    }

    private fun sliceBalancedArray(raw: String, start: Int): String? {
        var depth = 0
        for (i in start until raw.length) {
            when (raw[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    companion object {
        fun defaultHttpClient(url: String, body: String): String {
            val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
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
