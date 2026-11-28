package com.truckie1.routing

/**
 * Shared routing value types used by [MultiStopTourComposer] and
 * [TourPathSelector].
 *
 * Path geometry goes through [PathRouter] / [ValhallaPathRouterFactory].
 * Clearance / mass hard-blocks live on the composer + PathRouter truck model
 * path — the legacy SafeRouteSelector candidate filter was deleted (ATNA-92).
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
