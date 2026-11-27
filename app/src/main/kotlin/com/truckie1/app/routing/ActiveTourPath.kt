package com.truckie1.app.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route
import com.truckie1.trips.TourOptimizeGoals
import com.truckie1.trips.TripStop

/**
 * One active-tour geometry path for Navigate + map polyline (ATNA-87 / P1-2).
 *
 * Composes ordered [PathRouter][com.truckie1.routing.PathRouter] legs via
 * [MultiStopTourComposer] (start → stops → end). Prefer a cached Optimise
 * [RoutePath] when present. Never a start→end-only stub that skips stops.
 */
object ActiveTourPath {

    sealed class Result {
        data class Ready(val path: RoutePath) : Result()
        data object MissingEndpoints : Result()
        data object MissingVehicle : Result()
        data class Blocked(val reason: String) : Result()
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

    /**
     * Resolve constrained tour geometry for [route].
     *
     * Preference: cached [optimisedPath] → [MultiStopTourComposer.composeOrdered]
     * for the current stop order (same PathRouter as Optimise / polyline).
     */
    fun compose(
        route: Route,
        vehicle: Vehicle?,
        bufferM: Double,
        composer: MultiStopTourComposer,
        optimisedPath: RoutePath? = null,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
    ): Result {
        if (!route.hasEndpoints) {
            return Result.MissingEndpoints
        }
        optimisedPath?.takeIf { it.points.size >= 2 }?.let {
            return Result.Ready(it)
        }

        val tourStops = tourStopsFrom(route)
        if (tourStops.size < 2) {
            return Result.MissingEndpoints
        }
        if (vehicle == null) {
            return Result.MissingVehicle
        }

        return when (
            val composed = composer.composeOrdered(
                stops = tourStops,
                vehicle = vehicle,
                bufferM = bufferM,
                goals = goals,
            )
        ) {
            is OptimizeResult.Ok -> {
                val path = composed.path
                    ?: return Result.Blocked("Tour missing PathRouter geometry")
                Result.Ready(path)
            }
            is OptimizeResult.Blocked -> Result.Blocked(composed.reason)
        }
    }
}
