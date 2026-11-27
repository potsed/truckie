package com.truckie1.routing

/**
 * Shared GraphHopper routing value types used by [MultiStopTourComposer] and
 * [SafeRouteSelector] (legacy candidate filter for `ff_bridge_safe_routing`).
 *
 * Path geometry goes through [PathRouter] / [GraphHopperPathRouterFactory].
 * The old [Router] / StubGraphHopperRouter / VehicleTripOptimizer path was
 * removed in ATNA-71 — do not reintroduce a parallel optimiser.
 */

enum class TollPreference {
    AVOID_ALL,
    ALLOW_SOME,
    ALLOW_ALL,
}

data class RouteCandidate(
    val id: String,
    val durationSeconds: Long,
    val costCents: Long,
    val maxHeightM: Double,
    val maxWeightT: Double,
)

sealed class RouteResult {
    data class Ok(val selected: RouteCandidate, val alternatives: List<RouteCandidate> = emptyList()) : RouteResult()
    data class Blocked(val reason: String) : RouteResult()
}
