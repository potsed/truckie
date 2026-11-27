package com.truckie1.routing

import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop

/**
 * Result of multi-stop GraphHopper tour optimisation ([MultiStopTourComposer]).
 */
sealed class OptimizeResult {
    data class Ok(
        val orderedStops: List<TripStop>,
        val route: RouteCandidate,
        val mode: OptimizeMode,
        val customModel: GraphHopperCustomModelStub,
        /** Composed GraphHopper path for the winning tour. */
        val path: RoutePath? = null,
    ) : OptimizeResult()

    data class Blocked(val reason: String) : OptimizeResult()
}
