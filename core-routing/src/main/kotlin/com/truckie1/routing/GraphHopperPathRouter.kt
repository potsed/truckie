package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint

/**
 * GraphHopper path for Navigate (start → end) on the MapLibre map.
 *
 * Always runs in-app with [TruckRoutingPreferences] (bridges, weight, tolls,
 * residential avoidance). Live HTTP GraphHopper is not wired yet —
 * [StubGraphHopperPathRouter] returns synthetic geometry and records the
 * Custom Model request shape for Phase D.
 */
interface PathRouter {
    fun path(
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
    override fun path(
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
            ?: GraphHopperCustomModelStub(
                requiredClearanceHeightM = 4.0,
                requiredMassT = 12.0,
                vehicleWidthM = 2.5,
                vehicleLengthM = 12.0,
                preferences = preferences,
            ).toRequestSnippet()
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
        // Rough stub: ~1 minute per segment.
        return (points.size - 1).coerceAtLeast(1) * 60L
    }
}
