package com.truckie1.routing

import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TourOptimizeGoals

/**
 * Selects among candidates that hard-satisfy effective clearance height and
 * required operating mass. Uses [TourOptimizeGoals] checkboxes (Fastest + Cost
 * Effective, both on by default = AND) via [TourPathSelector].
 */
class SafeRouteSelector {
    fun select(
        candidates: List<RouteCandidate>,
        requiredClearanceHeightM: Double,
        requiredMassT: Double,
        mode: OptimizeMode,
    ): RouteResult = select(
        candidates = candidates,
        requiredClearanceHeightM = requiredClearanceHeightM,
        requiredMassT = requiredMassT,
        goals = TourOptimizeGoals.fromLegacyMode(mode),
    )

    fun select(
        candidates: List<RouteCandidate>,
        requiredClearanceHeightM: Double,
        requiredMassT: Double,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
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
        val selected = requireNotNull(TourPathSelector.select(safe, goals))
        return RouteResult.Ok(selected = selected, alternatives = safe - selected)
    }
}
