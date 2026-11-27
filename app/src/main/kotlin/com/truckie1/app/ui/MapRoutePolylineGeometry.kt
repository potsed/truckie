package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route
import com.truckie1.trips.TourOptimizeGoals
import com.truckie1.trips.TripStop

/**
 * Resolves MapLibre home route polyline geometry from PathRouter /
 * MultiStopTourComposer — never DIY straight-lines between stops.
 *
 * Preference order:
 * 1. Navigate active [RoutePath] (already PathRouter)
 * 2. Cached optimised tour geometry when available
 * 3. [MultiStopTourComposer.optimize] with [TourOptimizeGoals.DEFAULT]
 *    (Fastest + Cost Effective checkboxes both on = AND)
 *
 * Toll preference applied when `ff_tolls`; under-passage / weight via the same
 * composer + BridgePassagePolicy + pack bridges as Optimize/Navigate.
 */
object MapRoutePolylineGeometry {

    sealed class Result {
        data class Ready(val path: RoutePath) : Result()
        data object FlagOff : Result()
        data object MissingEndpoints : Result()
        data class Blocked(val reason: String) : Result()
    }

    fun pointsOrEmpty(result: Result): List<LatLngPoint> = when (result) {
        is Result.Ready -> result.path.points
        else -> emptyList()
    }

    /**
     * Tour waypoints: start → ordered stops → end (verified coords only).
     * Round-trip end is included so the return leg is PathRouter-composed.
     */
    fun tourStopsFrom(route: Route): List<TripStop> {
        val out = mutableListOf<TripStop>()
        route.start?.let { start ->
            out += TripStop(
                id = "start",
                addressLabel = start.addressLabel,
                latitude = start.latitude,
                longitude = start.longitude,
            )
        }
        for (stop in route.stops) {
            val lat = stop.latitude ?: continue
            val lng = stop.longitude ?: continue
            out += TripStop(
                id = stop.id,
                addressLabel = stop.addressLabel,
                latitude = lat,
                longitude = lng,
                cargo = stop.cargo,
            )
        }
        route.end?.let { end ->
            out += TripStop(
                id = "end",
                addressLabel = end.addressLabel,
                latitude = end.latitude,
                longitude = end.longitude,
            )
        }
        return out
    }

    fun resolve(
        flags: FeatureFlags,
        mapHomeEnabled: Boolean,
        route: Route?,
        vehicle: Vehicle?,
        bufferM: Double,
        /**
         * Same app [PathRouter] used to construct [composer] (Navigate/Optimize).
         * Geometry always flows through that router — never DIY polylines.
         */
        @Suppress("UNUSED_PARAMETER") pathRouter: PathRouter,
        composer: MultiStopTourComposer,
        optimisedPath: RoutePath? = null,
        navigatePath: RoutePath? = null,
        goals: TourOptimizeGoals = MapRoutePolylineCatalog.defaultTourGoals,
    ): Result {
        if (!MapRoutePolylineCatalog.shouldShow(flags, mapHomeEnabled)) {
            return Result.FlagOff
        }
        if (route == null || !route.hasEndpoints) {
            return Result.MissingEndpoints
        }

        navigatePath?.takeIf { it.points.size >= 2 }?.let {
            return Result.Ready(it)
        }
        optimisedPath?.takeIf { it.points.size >= 2 }?.let {
            return Result.Ready(it)
        }

        val tourStops = tourStopsFrom(route)
        if (tourStops.size < 2) {
            return Result.MissingEndpoints
        }
        if (vehicle == null) {
            return Result.Blocked("Active vehicle required for constrained map route")
        }

        // Toll preference lives on the injected composer (same as Optimize).
        require(MapRoutePolylineCatalog.usesSamePathRouterAsOptimizeAndNavigate)
        val composed = composer.optimize(
            stops = tourStops,
            vehicle = vehicle,
            bufferM = bufferM,
            goals = goals,
        )
        return when (composed) {
            is OptimizeResult.Ok -> {
                val path = composed.path
                    ?: return Result.Blocked("Optimised tour missing PathRouter geometry")
                Result.Ready(path)
            }
            is OptimizeResult.Blocked -> Result.Blocked(composed.reason)
        }
    }
}
