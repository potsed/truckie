package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Trip

/**
 * GraphHopper client contract. Phase 0 ships a stub only.
 */
interface Router {
    fun route(
        trip: Trip,
        vehicle: Vehicle,
        effectiveClearanceHeightM: Double,
        mode: OptimizeMode,
        tollPreference: TollPreference,
    ): RouteResult
}

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

/**
 * Stub GraphHopper client for assemble/tests. Rejects candidates below clearance or mass
 * via [SafeRouteSelector].
 */
class StubGraphHopperRouter(
    private val candidatesProvider: (Trip) -> List<RouteCandidate> = { trip ->
        DefaultTourCandidates.forStops(trip.stops.size)
    },
    private val selector: SafeRouteSelector = SafeRouteSelector(),
) : Router {
    override fun route(
        trip: Trip,
        vehicle: Vehicle,
        effectiveClearanceHeightM: Double,
        mode: OptimizeMode,
        tollPreference: TollPreference,
    ): RouteResult = selector.select(
        candidates = candidatesProvider(trip),
        requiredClearanceHeightM = effectiveClearanceHeightM,
        requiredMassT = vehicle.grossMassT,
        mode = mode,
    )
}

/**
 * Synthetic GraphHopper alternatives for dogfood / offline stub.
 * Primary is faster; alternate is cheaper. Both allow generous height/weight
 * so real Custom Model filtering happens in [SafeRouteSelector] / [VehicleTripOptimizer].
 */
object DefaultTourCandidates {
    fun forStops(stopCount: Int, orderKey: Int = 0): List<RouteCandidate> {
        val n = stopCount.coerceAtLeast(1)
        val baseDuration = 1_000L * n + orderKey * 100L
        val baseCost = 100L * n + (10 - orderKey).coerceAtLeast(0) * 10L
        return listOf(
            RouteCandidate(
                id = "primary-$orderKey",
                durationSeconds = baseDuration,
                costCents = baseCost + 80,
                maxHeightM = 5.0,
                maxWeightT = 50.0,
            ),
            RouteCandidate(
                id = "alt-$orderKey",
                durationSeconds = baseDuration + 2_000L,
                costCents = baseCost,
                maxHeightM = 5.0,
                maxWeightT = 50.0,
            ),
        )
    }
}
