package com.truckie1.routing

import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop

/**
 * Result of multi-stop PathRouter tour optimisation ([MultiStopTourComposer]).
 */
sealed class OptimizeResult {
    data class Ok(
        val orderedStops: List<TripStop>,
        val route: RouteCandidate,
        val mode: OptimizeMode,
        val customModel: TruckRoutingModel,
        /** Composed PathRouter path for the winning tour. */
        val path: RoutePath? = null,
    ) : OptimizeResult()

    data class Blocked(val reason: String) : OptimizeResult()
}
