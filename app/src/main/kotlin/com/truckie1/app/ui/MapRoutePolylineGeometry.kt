package com.truckie1.app.ui

import com.truckie1.app.routing.ActiveTourPath
import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
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
 * 3. [ActiveTourPath.compose] ordered PathRouter legs (same as Navigate)
 *
 * With `ff_route_path_from_location` (ATNA-99): current → start → stops → end.
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
     * Delegates to [ActiveTourPath] so Navigate and polyline share one SoT.
     */
    fun tourStopsFrom(route: Route): List<TripStop> = ActiveTourPath.tourStopsFrom(route)

    suspend fun resolve(
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
        currentLocation: LatLngPoint? = null,
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

        require(MapRoutePolylineCatalog.usesSamePathRouterAsOptimizeAndNavigate)
        return when (
            val composed = ActiveTourPath.compose(
                route = route,
                vehicle = vehicle,
                bufferM = bufferM,
                composer = composer,
                optimisedPath = optimisedPath,
                goals = goals,
                currentLocation = RoutePathFromLocationCatalog.approachFix(
                    flags = flags,
                    currentLocation = currentLocation,
                ),
            )
        ) {
            is ActiveTourPath.Result.Ready -> Result.Ready(composed.path)
            is ActiveTourPath.Result.MissingEndpoints -> Result.MissingEndpoints
            is ActiveTourPath.Result.MissingVehicle ->
                Result.Blocked("Active vehicle required for constrained map route")
            is ActiveTourPath.Result.Blocked -> Result.Blocked(composed.reason)
        }
    }
}
