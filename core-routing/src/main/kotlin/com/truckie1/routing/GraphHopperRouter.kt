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
 * Stub GraphHopper client for assemble/tests. Rejects candidates below clearance or mass.
 */
class StubGraphHopperRouter(
    private val candidatesProvider: (Trip) -> List<RouteCandidate> = { emptyList() },
) : Router {
    override fun route(
        trip: Trip,
        vehicle: Vehicle,
        effectiveClearanceHeightM: Double,
        mode: OptimizeMode,
        tollPreference: TollPreference,
    ): RouteResult {
        val safe = candidatesProvider(trip).filter { candidate ->
            candidate.maxHeightM >= effectiveClearanceHeightM &&
                candidate.maxWeightT >= vehicle.grossMassT
        }
        if (safe.isEmpty()) {
            return RouteResult.Blocked(
                reason = "No safe route for clearance ${"%.2f".format(effectiveClearanceHeightM)} m " +
                    "and mass ${vehicle.grossMassT} t",
            )
        }
        val selected = when (mode) {
            OptimizeMode.FASTEST -> safe.minBy { it.durationSeconds }
            OptimizeMode.CHEAPEST -> safe.minBy { it.costCents }
        }
        return RouteResult.Ok(selected = selected, alternatives = safe - selected)
    }
}
