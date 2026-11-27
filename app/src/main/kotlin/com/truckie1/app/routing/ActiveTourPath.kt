package com.truckie1.app.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route
import com.truckie1.trips.TourOptimizeGoals
import com.truckie1.trips.TripStop
import kotlin.math.abs

/**
 * One active-tour geometry path for Navigate + map polyline (ATNA-87 / ATNA-99).
 *
 * Composes ordered [PathRouter][com.truckie1.routing.PathRouter] legs via
 * [MultiStopTourComposer]: optional current location → start → stops → end.
 * Prefer a cached Optimise [RoutePath] when present (approach leg prepended
 * when a current fix is supplied). Never a start→end-only stub that skips stops.
 */
object ActiveTourPath {

    const val CURRENT_LOCATION_ID: String = "current"

    /** Degrees — ~55 m; skip approach when already at route start. */
    private const val SAME_PLACE_EPS: Double = 0.0005

    sealed class Result {
        data class Ready(val path: RoutePath) : Result()
        data object MissingEndpoints : Result()
        data object MissingVehicle : Result()
        data class Blocked(val reason: String) : Result()
    }

    /**
     * Tour waypoints for Optimise: start → ordered stops → end (verified coords).
     * Does **not** include current location — Optimise keeps start as fixed origin.
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
     * Display / Navigate waypoints: current (when provided and distinct) →
     * [tourStopsFrom].
     */
    fun orderedWaypoints(
        route: Route,
        currentLocation: LatLngPoint? = null,
    ): List<TripStop> {
        val tour = tourStopsFrom(route)
        if (currentLocation == null) return tour
        val first = tour.firstOrNull() ?: return tour
        val firstLat = first.latitude ?: return tour
        val firstLng = first.longitude ?: return tour
        if (near(currentLocation, firstLat, firstLng)) return tour
        return listOf(currentAsStop(currentLocation)) + tour
    }

    /**
     * Resolve constrained tour geometry for [route].
     *
     * Preference: cached [optimisedPath] (with approach prepended when
     * [currentLocation] is set) → [MultiStopTourComposer.composeOrdered] for
     * [orderedWaypoints].
     */
    fun compose(
        route: Route,
        vehicle: Vehicle?,
        bufferM: Double,
        composer: MultiStopTourComposer,
        optimisedPath: RoutePath? = null,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
        currentLocation: LatLngPoint? = null,
    ): Result {
        if (!route.hasEndpoints) {
            return Result.MissingEndpoints
        }
        if (vehicle == null) {
            return Result.MissingVehicle
        }

        optimisedPath?.takeIf { it.points.size >= 2 }?.let { cached ->
            return withApproach(
                tourPath = cached,
                route = route,
                vehicle = vehicle,
                bufferM = bufferM,
                composer = composer,
                goals = goals,
                currentLocation = currentLocation,
            )
        }

        val waypoints = orderedWaypoints(route, currentLocation)
        if (waypoints.size < 2) {
            return Result.MissingEndpoints
        }

        return when (
            val composed = composer.composeOrdered(
                stops = waypoints,
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

    /**
     * Prepend current → tour-start PathRouter leg onto a cached Optimise path.
     * When no current fix (or already at start), returns [tourPath] unchanged.
     */
    private fun withApproach(
        tourPath: RoutePath,
        route: Route,
        vehicle: Vehicle,
        bufferM: Double,
        composer: MultiStopTourComposer,
        goals: TourOptimizeGoals,
        currentLocation: LatLngPoint?,
    ): Result {
        val waypoints = orderedWaypoints(route, currentLocation)
        if (waypoints.isEmpty() || waypoints.first().id != CURRENT_LOCATION_ID) {
            return Result.Ready(tourPath)
        }
        val approachStops = waypoints.take(2)
        return when (
            val approach = composer.composeOrdered(
                stops = approachStops,
                vehicle = vehicle,
                bufferM = bufferM,
                goals = goals,
            )
        ) {
            is OptimizeResult.Ok -> {
                val approachPath = approach.path
                    ?: return Result.Blocked("Approach missing PathRouter geometry")
                Result.Ready(stitch(approachPath, tourPath))
            }
            is OptimizeResult.Blocked -> Result.Blocked(approach.reason)
        }
    }

    private fun stitch(approach: RoutePath, tour: RoutePath): RoutePath {
        val points = approach.points + tour.points.drop(1)
        return RoutePath(
            id = "approach+${tour.id}",
            points = points,
            durationSeconds = (approach.durationSeconds + tour.durationSeconds).coerceAtLeast(1L),
            source = tour.source,
            preferences = tour.preferences,
            customModelSnippet = tour.customModelSnippet ?: approach.customModelSnippet,
        )
    }

    private fun currentAsStop(current: LatLngPoint): TripStop = TripStop(
        id = CURRENT_LOCATION_ID,
        addressLabel = "Current location",
        latitude = current.latitude,
        longitude = current.longitude,
    )

    private fun near(point: LatLngPoint, lat: Double, lng: Double): Boolean =
        abs(point.latitude - lat) < SAME_PLACE_EPS &&
            abs(point.longitude - lng) < SAME_PLACE_EPS
}
