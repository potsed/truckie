package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint

/**
 * Alternate around geographic path blocks via [PathRouter] (ATNA-102).
 *
 * Production path: **live Valhalla** truck costing / exclude polygons
 * (`ff_valhalla_live` + credentials). When a leg is Blocked or the composed
 * points conflict with under-passage pack bridges, re-invoke [PathRouter.path]
 * with the same truck model so Valhalla (or the stub under test) returns an
 * alternate — never a hand-rolled MapLibre polyline rule, and never erase the
 * tour line when PathRouter can still produce ≥2 points.
 *
 * Stub PathRouter may simulate blocked-edge avoidance for unit tests only.
 *
 * Gherkin: features/path_block_detour.feature
 */
object PathBlockDetour {
    /** Production design uses Valhalla PathRouter — not DIY polylines. */
    const val usesPathRouter: Boolean = true
    const val usesHandRolledPolylineRule: Boolean = false
    const val prefersLiveValhalla: Boolean = true

    /**
     * Resolve one leg: first PathRouter attempt, then PathRouter retry with
     * [avoidUnderBridges] when blocked or under-passage conflict. Prefers any
     * PathRouter [PathResult.Ok] (≥2 points) over blanking the tour.
     */
    suspend fun resolveLeg(
        pathRouter: PathRouter,
        start: RouteEndpoint,
        end: RouteEndpoint,
        preferences: TruckRoutingPreferences,
        customModel: TruckRoutingModel?,
        bridges: List<RoutingBridgeConstraint>,
        vehicleHeightM: Double,
        bufferM: Double,
        allowedNearPoints: List<LatLngPoint> = emptyList(),
        bridgePassageEnabled: Boolean = false,
    ): PathResult {
        val model = customModel
            ?: TruckRoutingModel.default(preferences = preferences)
        val first = pathRouter.path(
            start = start,
            end = end,
            preferences = preferences,
            customModel = model,
        )
        when (first) {
            is PathResult.Ok -> {
                if (!bridgePassageEnabled ||
                    !BridgePassageConstraint.pathConflictsWithUnderBridges(
                        points = first.path.points,
                        bridges = bridges,
                        vehicleHeightM = vehicleHeightM,
                        bufferM = bufferM,
                        allowedNearPoints = allowedNearPoints,
                    )
                ) {
                    return first
                }
                val avoid = conflictingBridges(
                    points = first.path.points,
                    bridges = bridges,
                    vehicleHeightM = vehicleHeightM,
                    bufferM = bufferM,
                    allowedNearPoints = allowedNearPoints,
                )
                val retry = pathRouter.path(
                    start = start,
                    end = end,
                    preferences = preferences,
                    customModel = model,
                    avoidUnderBridges = avoid.ifEmpty { bridges },
                )
                return when (retry) {
                    is PathResult.Ok -> retry
                    // Keep the drawable PathRouter geometry — never erase the line.
                    is PathResult.Blocked -> first
                }
            }
            is PathResult.Blocked -> {
                val retry = pathRouter.path(
                    start = start,
                    end = end,
                    preferences = preferences,
                    customModel = model,
                    avoidUnderBridges = bridges,
                )
                return when (retry) {
                    is PathResult.Ok -> retry
                    is PathResult.Blocked -> first
                }
            }
        }
    }

    fun conflictingBridges(
        points: List<LatLngPoint>,
        bridges: List<RoutingBridgeConstraint>,
        vehicleHeightM: Double,
        bufferM: Double,
        allowedNearPoints: List<LatLngPoint> = emptyList(),
    ): List<RoutingBridgeConstraint> {
        if (points.isEmpty()) return emptyList()
        return BridgePassagePolicy.bridgesRestrictingUnder(bridges).filter { bridge ->
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = bridge.clearanceHeightM,
                structurePassage = bridge.passage,
                vehicleHeightM = vehicleHeightM,
                bufferM = bufferM,
                pathRelation = BridgePassageRelation.UNDER,
            ) && points.any { point ->
                !isNearAllowed(point, allowedNearPoints) &&
                    nearBridge(point, bridge)
            }
        }
    }

    private fun isNearAllowed(
        point: LatLngPoint,
        allowedNearPoints: List<LatLngPoint>,
    ): Boolean =
        allowedNearPoints.any { stop ->
            haversineM(
                point.latitude,
                point.longitude,
                stop.latitude,
                stop.longitude,
            ) <= BridgePassagePolicy.ALLOWED_STOP_NEAR_M
        }

    private fun nearBridge(point: LatLngPoint, bridge: RoutingBridgeConstraint): Boolean =
        haversineM(
            point.latitude,
            point.longitude,
            bridge.latitude,
            bridge.longitude,
        ) <= BridgePassagePolicy.NEAR_PATH_M

    private fun haversineM(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
