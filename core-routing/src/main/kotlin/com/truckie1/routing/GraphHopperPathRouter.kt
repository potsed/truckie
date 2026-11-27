package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint

/**
 * GraphHopper path leg used by [MultiStopTourComposer] / ActiveTourPath
 * (Navigate + map polyline) and Optimise.
 *
 * Always runs in-app with [TruckRoutingPreferences] (bridges, weight, tolls,
 * residential avoidance). [path] is **suspend** (ATNA-88): live HTTP hops off
 * the caller thread; [StubGraphHopperPathRouter] stays instantaneous.
 */
interface PathRouter {
    suspend fun path(
        start: RouteEndpoint,
        end: RouteEndpoint,
        preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT,
        customModel: GraphHopperCustomModelStub? = null,
    ): PathResult
}

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
)

data class RoutePath(
    val id: String,
    val points: List<LatLngPoint>,
    val durationSeconds: Long = 0L,
    /** `graphhopper-stub` until live GraphHopper HTTP is enabled. */
    val source: String = SOURCE_STUB,
    val preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT,
    /** Custom Model snippet that live GraphHopper will receive (Phase D). */
    val customModelSnippet: String? = null,
) {
    init {
        require(points.size >= 2) { "path needs at least start and end points" }
    }

    companion object {
        const val SOURCE_STUB: String = "graphhopper-stub"
        const val SOURCE_LIVE: String = "graphhopper-live"
    }
}

sealed class PathResult {
    data class Ok(val path: RoutePath) : PathResult()
    data class Blocked(val reason: String) : PathResult()
}

/**
 * Offline stub: interpolates a simple path from start to end (and a short loop
 * for round trips) so Navigate can render on MapLibre without a live GH server.
 * Records [TruckRoutingPreferences] / Custom Model shape for constrained truck routing.
 */
class StubGraphHopperPathRouter(
    private val intermediatePoints: Int = 6,
) : PathRouter {
    override suspend fun path(
        start: RouteEndpoint,
        end: RouteEndpoint,
        preferences: TruckRoutingPreferences,
        customModel: GraphHopperCustomModelStub?,
    ): PathResult {
        val points = if (start.samePlaceAs(end)) {
            roundTripLoop(start)
        } else {
            interpolate(start, end, intermediatePoints)
        }
        val modelSnippet = customModel?.toRequestSnippet()
            ?: GraphHopperCustomModelStub.default(preferences = preferences).toRequestSnippet()
        return PathResult.Ok(
            RoutePath(
                id = "stub-${start.latitude},${start.longitude}->${end.latitude},${end.longitude}",
                points = points,
                durationSeconds = estimateDurationSeconds(points),
                source = RoutePath.SOURCE_STUB,
                preferences = preferences,
                customModelSnippet = modelSnippet,
            ),
        )
    }

    private fun interpolate(
        start: RouteEndpoint,
        end: RouteEndpoint,
        steps: Int,
    ): List<LatLngPoint> {
        val n = steps.coerceAtLeast(1)
        val points = ArrayList<LatLngPoint>(n + 1)
        for (i in 0..n) {
            val t = i.toDouble() / n
            points += LatLngPoint(
                latitude = start.latitude + (end.latitude - start.latitude) * t,
                longitude = start.longitude + (end.longitude - start.longitude) * t,
            )
        }
        return points
    }

    /** Tiny diamond loop so round-trip Navigate still draws a visible path. */
    private fun roundTripLoop(place: RouteEndpoint): List<LatLngPoint> {
        val d = 0.004
        return listOf(
            LatLngPoint(place.latitude, place.longitude),
            LatLngPoint(place.latitude + d, place.longitude),
            LatLngPoint(place.latitude + d, place.longitude + d),
            LatLngPoint(place.latitude, place.longitude + d),
            LatLngPoint(place.latitude, place.longitude),
        )
    }

    private fun estimateDurationSeconds(points: List<LatLngPoint>): Long {
        // Distance-based stub so multi-stop optimisation can prefer shorter tours.
        // ~50 km/h average → 13.89 m/s.
        var metres = 0.0
        for (i in 1 until points.size) {
            metres += haversineM(points[i - 1], points[i])
        }
        val seconds = (metres / 13.89).toLong()
        return seconds.coerceAtLeast(1L)
    }

    private fun haversineM(a: LatLngPoint, b: LatLngPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = kotlin.math.sin(dLat / 2)
        val sinLon = kotlin.math.sin(dLon / 2)
        val h = sinLat * sinLat +
            kotlin.math.cos(Math.toRadians(a.latitude)) *
            kotlin.math.cos(Math.toRadians(b.latitude)) *
            sinLon * sinLon
        return 2 * r * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
    }
}
