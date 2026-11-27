package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OnBoardMass
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.TourOptimizeGoals
import com.truckie1.trips.TripStop

/**
 * Multi-stop GraphHopper tour optimiser (ATNA-68 / ATNA-82).
 *
 * Origin (first stop) stays fixed; remaining stops are permuted. Each order is
 * scored by composing [PathRouter] legs with a truck Custom Model. Under-passage
 * low bridges hard-block when [bridgePassageEnabled]; over-deck remains OK.
 *
 * Selection uses [TourOptimizeGoals] checkboxes (Fastest + Cost Effective, both
 * on by default = AND) — not single-axis radio.
 */
class MultiStopTourComposer(
    private val pathRouter: PathRouter,
    private val bridges: List<RoutingBridgeConstraint> = emptyList(),
    private val bridgePassageEnabled: Boolean = false,
    private val tollPreference: TollPreference = TollPreference.ALLOW_ALL,
    private val preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT,
) {
    fun optimize(
        stops: List<TripStop>,
        vehicle: Vehicle,
        bufferM: Double,
        mode: OptimizeMode,
    ): OptimizeResult = optimize(
        stops = stops,
        vehicle = vehicle,
        bufferM = bufferM,
        goals = TourOptimizeGoals.fromLegacyMode(mode),
        legacyMode = mode,
    )

    fun optimize(
        stops: List<TripStop>,
        vehicle: Vehicle,
        bufferM: Double,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
        legacyMode: OptimizeMode = goals.asLegacyMode() ?: OptimizeMode.FASTEST,
    ): OptimizeResult {
        if (stops.isEmpty()) {
            return OptimizeResult.Blocked(reason = "Add at least one stop before optimizing")
        }
        if (stops.any { it.latitude == null || it.longitude == null }) {
            return OptimizeResult.Blocked(
                reason = "All stops need verified coordinates before GraphHopper optimisation",
            )
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
            val modelPrefs = preferences.copy(
                tollPreference = tollPreference,
                optimizeMode = legacyMode,
            )
            val model = GraphHopperCustomModelStub.from(
                vehicle = vehicle,
                requiredClearanceHeightM = requiredClearance,
                requiredMassT = requiredMass,
                tollPreference = tollPreference,
                preferences = modelPrefs,
            )

            when (val composed = composeTour(ordered, model, vehicle.heightM, bufferM)) {
                is TourCompose.Blocked -> blockers += composed.reason
                is TourCompose.Ok -> {
                    val candidate = OptimizeResult.Ok(
                        orderedStops = ordered,
                        route = composed.route,
                        mode = legacyMode,
                        customModel = model,
                        path = composed.path,
                    )
                    best = TourPathSelector.pickBetter(
                        current = best,
                        next = candidate,
                        candidateOf = { it.route },
                        goals = goals,
                    )
                }
            }
        }

        return best ?: OptimizeResult.Blocked(
            reason = blockers.firstOrNull()
                ?: "No safe GraphHopper tour for the active vehicle",
        )
    }

    /**
     * Compose PathRouter legs in the **given** stop order (no permutation).
     * Same Custom Model + under-passage hard-block as [optimize].
     */
    fun composeOrdered(
        stops: List<TripStop>,
        vehicle: Vehicle,
        bufferM: Double,
        mode: OptimizeMode = OptimizeMode.FASTEST,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
    ): OptimizeResult {
        if (stops.isEmpty()) {
            return OptimizeResult.Blocked(reason = "Add at least one stop before composing")
        }
        if (stops.any { it.latitude == null || it.longitude == null }) {
            return OptimizeResult.Blocked(
                reason = "All stops need verified coordinates before GraphHopper composition",
            )
        }
        if (OnBoardMass.exceedsGvm(tareT = vehicle.tareT, gvmT = vehicle.gvmT, stops = stops)) {
            val peak = OnBoardMass.peakOperatingMassT(vehicle.tareT, stops)
            return OptimizeResult.Blocked(
                reason = "On-board mass ${"%.2f".format(peak)} t exceeds GVM ${vehicle.gvmT} t",
            )
        }
        val requiredClearance = ClearanceCalculator.maxEffectiveHeightAlongTripM(
            vehicleHeightM = vehicle.heightM,
            stops = stops,
            bufferM = bufferM,
        )
        val requiredMass = OnBoardMass.peakOperatingMassT(vehicle.tareT, stops)
        val legacyMode = goals.asLegacyMode() ?: mode
        val modelPrefs = preferences.copy(
            tollPreference = tollPreference,
            optimizeMode = legacyMode,
        )
        val model = GraphHopperCustomModelStub.from(
            vehicle = vehicle,
            requiredClearanceHeightM = requiredClearance,
            requiredMassT = requiredMass,
            tollPreference = tollPreference,
            preferences = modelPrefs,
        )
        return when (val composed = composeTour(stops, model, vehicle.heightM, bufferM)) {
            is TourCompose.Blocked -> OptimizeResult.Blocked(composed.reason)
            is TourCompose.Ok -> OptimizeResult.Ok(
                orderedStops = stops,
                route = composed.route,
                mode = legacyMode,
                customModel = model,
                path = composed.path,
            )
        }
    }

    private fun composeTour(
        ordered: List<TripStop>,
        model: GraphHopperCustomModelStub,
        vehicleHeightM: Double,
        bufferM: Double,
    ): TourCompose {
        if (ordered.size == 1) {
            val stop = ordered.first()
            val endpoint = stop.asEndpoint()
            return when (
                val leg = pathRouter.path(
                    start = endpoint,
                    end = endpoint,
                    preferences = model.preferences,
                    customModel = model,
                )
            ) {
                is PathResult.Blocked -> TourCompose.Blocked(leg.reason)
                is PathResult.Ok -> {
                    if (bridgePassageEnabled &&
                        BridgePassageConstraint.pathConflictsWithUnderBridges(
                            points = leg.path.points,
                            bridges = bridges,
                            vehicleHeightM = vehicleHeightM,
                            bufferM = bufferM,
                        )
                    ) {
                        return TourCompose.Blocked(
                            reason = "Under-passage bridge clearance conflict on tour",
                        )
                    }
                    TourCompose.Ok(
                        route = RouteCandidate(
                            id = leg.path.id,
                            durationSeconds = leg.path.durationSeconds,
                            costCents = estimateCostCents(leg.path.durationSeconds),
                            maxHeightM = 50.0,
                            maxWeightT = 50.0,
                        ),
                        path = leg.path,
                    )
                }
            }
        }

        val allPoints = mutableListOf<LatLngPoint>()
        var totalDuration = 0L
        var lastSource = RoutePath.SOURCE_STUB
        var lastSnippet: String? = null
        var lastPrefs = model.preferences

        for (i in 0 until ordered.lastIndex) {
            val from = ordered[i].asEndpoint()
            val to = ordered[i + 1].asEndpoint()
            when (
                val leg = pathRouter.path(
                    start = from,
                    end = to,
                    preferences = model.preferences,
                    customModel = model,
                )
            ) {
                is PathResult.Blocked -> return TourCompose.Blocked(leg.reason)
                is PathResult.Ok -> {
                    if (allPoints.isEmpty()) {
                        allPoints += leg.path.points
                    } else {
                        allPoints += leg.path.points.drop(1)
                    }
                    totalDuration += leg.path.durationSeconds
                    lastSource = leg.path.source
                    lastSnippet = leg.path.customModelSnippet
                    lastPrefs = leg.path.preferences
                }
            }
        }

        if (bridgePassageEnabled &&
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = allPoints,
                bridges = bridges,
                vehicleHeightM = vehicleHeightM,
                bufferM = bufferM,
            )
        ) {
            return TourCompose.Blocked(
                reason = "Under-passage bridge clearance conflict on tour",
            )
        }

        val path = RoutePath(
            id = "tour-${ordered.joinToString("-") { it.id }}",
            points = allPoints,
            durationSeconds = totalDuration.coerceAtLeast(1L),
            source = lastSource,
            preferences = lastPrefs,
            customModelSnippet = lastSnippet ?: model.toRequestSnippet(),
        )
        return TourCompose.Ok(
            route = RouteCandidate(
                id = path.id,
                durationSeconds = path.durationSeconds,
                // Stub cost: duration-proportional until live toll costs (ff_tolls).
                costCents = estimateCostCents(path.durationSeconds),
                maxHeightM = 50.0,
                maxWeightT = 50.0,
            ),
            path = path,
        )
    }

    private fun estimateCostCents(durationSeconds: Long): Long =
        (durationSeconds / 10L).coerceAtLeast(1L)

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

    private fun TripStop.asEndpoint(): RouteEndpoint = RouteEndpoint(
        addressLabel = addressLabel,
        latitude = requireNotNull(latitude),
        longitude = requireNotNull(longitude),
    )

    private sealed class TourCompose {
        data class Ok(val route: RouteCandidate, val path: RoutePath) : TourCompose()
        data class Blocked(val reason: String) : TourCompose()
    }
}
