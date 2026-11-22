package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OnBoardMass
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop

sealed class OptimizeResult {
    data class Ok(
        val orderedStops: List<TripStop>,
        val route: RouteCandidate,
        val mode: OptimizeMode,
        val customModel: GraphHopperCustomModelStub,
    ) : OptimizeResult()

    data class Blocked(val reason: String) : OptimizeResult()
}

/**
 * Finds Fastest/Cheapest among height- and weight-safe tours for the active vehicle.
 * First stop is fixed as origin; remaining stops are permuted. Persisting the chosen
 * [OptimizeResult.Ok.orderedStops] is the caller's responsibility (e.g. TripSession).
 */
class VehicleTripOptimizer(
    private val candidatesForOrder: (List<TripStop>) -> List<RouteCandidate>,
    private val selector: SafeRouteSelector = SafeRouteSelector(),
) {
    fun optimize(
        stops: List<TripStop>,
        vehicle: Vehicle,
        bufferM: Double,
        mode: OptimizeMode,
        tollPreference: TollPreference = TollPreference.ALLOW_ALL,
    ): OptimizeResult {
        if (stops.isEmpty()) {
            return OptimizeResult.Blocked(reason = "Add at least one stop before optimizing")
        }

        var best: OptimizeResult.Ok? = null
        val blockers = mutableListOf<String>()

        for (ordered in candidateOrders(stops)) {
            if (OnBoardMass.exceedsGvm(tareT = vehicle.tareT, gvmT = vehicle.gvmT, stops = ordered)) {
                val peak = OnBoardMass.peakOperatingMassT(vehicle.tareT, ordered)
                blockers += "On-board mass ${"%.2f".format(peak)} t exceeds GVM ${vehicle.gvmT} t"
                continue
            }

            val requiredClearance = ClearanceCalculator.maxEffectiveHeightAlongTripM(
                vehicleHeightM = vehicle.heightM,
                stops = ordered,
                bufferM = bufferM,
            )
            val requiredMass = OnBoardMass.peakOperatingMassT(vehicle.tareT, ordered)
            val model = GraphHopperCustomModelStub.from(
                vehicle = vehicle,
                requiredClearanceHeightM = requiredClearance,
                requiredMassT = requiredMass,
                tollPreference = tollPreference,
            )

            when (
                val selected = selector.select(
                    candidates = candidatesForOrder(ordered),
                    requiredClearanceHeightM = model.requiredClearanceHeightM,
                    requiredMassT = model.requiredMassT,
                    mode = mode,
                )
            ) {
                is RouteResult.Blocked -> blockers += selected.reason
                is RouteResult.Ok -> {
                    val candidate = OptimizeResult.Ok(
                        orderedStops = ordered,
                        route = selected.selected,
                        mode = mode,
                        customModel = model,
                    )
                    best = pickBetter(best, candidate, mode)
                }
            }
        }

        return best ?: OptimizeResult.Blocked(
            reason = blockers.firstOrNull()
                ?: "No safe route for the active vehicle and on-board cargo",
        )
    }

    private fun pickBetter(
        current: OptimizeResult.Ok?,
        next: OptimizeResult.Ok,
        mode: OptimizeMode,
    ): OptimizeResult.Ok {
        if (current == null) return next
        return when (mode) {
            OptimizeMode.FASTEST ->
                if (next.route.durationSeconds < current.route.durationSeconds) next else current
            OptimizeMode.CHEAPEST ->
                if (next.route.costCents < current.route.costCents) next else current
        }
    }

    /**
     * Origin (first stop) stays fixed; permute the rest so drop/pick sequences can improve.
     */
    private fun candidateOrders(stops: List<TripStop>): List<List<TripStop>> {
        if (stops.size <= 1) return listOf(stops)
        val origin = stops.first()
        val rest = stops.drop(1)
        return permutations(rest).map { perm -> listOf(origin) + perm }
    }

    private fun <T> permutations(items: List<T>): List<List<T>> {
        if (items.size <= 1) return listOf(items)
        val result = mutableListOf<List<T>>()
        for (i in items.indices) {
            val head = items[i]
            val others = items.filterIndexed { index, _ -> index != i }
            for (tail in permutations(others)) {
                result += listOf(head) + tail
            }
        }
        return result
    }
}
