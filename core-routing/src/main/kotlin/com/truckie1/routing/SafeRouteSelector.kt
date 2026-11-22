package com.truckie1.routing

import com.truckie1.trips.OptimizeMode

/**
 * Selects Fastest or Cheapest among candidates that hard-satisfy
 * effective clearance height and required operating mass.
 */
class SafeRouteSelector {
    fun select(
        candidates: List<RouteCandidate>,
        requiredClearanceHeightM: Double,
        requiredMassT: Double,
        mode: OptimizeMode,
    ): RouteResult {
        val safe = candidates.filter { candidate ->
            candidate.maxHeightM >= requiredClearanceHeightM &&
                candidate.maxWeightT >= requiredMassT
        }
        if (safe.isEmpty()) {
            return RouteResult.Blocked(
                reason = "No safe route for clearance ${"%.2f".format(requiredClearanceHeightM)} m " +
                    "and mass ${"%.2f".format(requiredMassT)} t",
            )
        }
        val selected = when (mode) {
            OptimizeMode.FASTEST -> safe.minBy { it.durationSeconds }
            OptimizeMode.CHEAPEST -> safe.minBy { it.costCents }
        }
        return RouteResult.Ok(selected = selected, alternatives = safe - selected)
    }
}
