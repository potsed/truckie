package com.truckie1.app.trips

import com.truckie1.app.routing.ActiveTourPath
import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.app.ui.NavigateMultiStopCatalog
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint

/**
 * Catalog for home Navigate (`ff_navigate`).
 *
 * Path finding is **always** in-app GraphHopper (stub until live HTTP) so vehicle
 * constraints can apply. Settings may select an installed nav app for **optional
 * post-route handoff** only — never as a substitute for GraphHopper.
 *
 * With `ff_navigate_multi_stop` (defaults on), geometry comes from
 * [ActiveTourPath] / [MultiStopTourComposer] — same ordered PathRouter legs as
 * Optimise + map polyline (ATNA-87). Start→end-only Navigate is deleted.
 */
object RouteNavigateCatalog {
    const val FLAG: String = FlagKeys.NAVIGATE

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NAVIGATE)

    fun canNavigate(
        flags: FeatureFlags,
        route: Route?,
        vehicle: Vehicle? = null,
    ): Boolean {
        if (!isEnabled(flags) || route == null || !route.hasEndpoints) return false
        if (NavigateMultiStopCatalog.isEnabled(flags) && vehicle == null) return false
        return true
    }

    fun showsNavigateAction(flags: FeatureFlags): Boolean = isEnabled(flags)
}

/**
 * Starts Navigate: always computes a constrained GraphHopper tour in-app,
 * draws it on MapLibre, then optionally offers external handoff when Settings
 * selects an installed app.
 *
 * [composerProvider] must be the app-owned [MultiStopTourComposer] (same as
 * Optimise / polyline). Production wiring: `TruckieApp.routeOptimizer`.
 */
class RouteNavigateSession(
    private val composerProvider: () -> MultiStopTourComposer,
) {
    private var activePath: RoutePath? = null

    fun activePath(): RoutePath? = activePath

    fun clear() {
        activePath = null
    }

    sealed class Result {
        /**
         * Constrained path ready on the map. [externalHandoff] is non-null only when
         * Settings selected an external app — handoff uses this calculated path's
         * destination/waypoints and must not re-route externally.
         */
        data class Ready(
            val path: RoutePath,
            val externalHandoff: ExternalHandoff? = null,
        ) : Result()

        data object FlagOff : Result()
        data object MissingEndpoints : Result()
        data class Blocked(val reason: String) : Result()
    }

    data class ExternalHandoff(
        val packageName: String,
        val label: String,
        /** Destination from the calculated path (route end). */
        val destination: RouteEndpoint,
        /** Waypoints from the GraphHopper path for optional handoff. */
        val waypoints: List<RouteEndpoint>,
    )

    fun start(
        flags: FeatureFlags,
        route: Route?,
        vehicle: Vehicle?,
        bufferM: Double,
        selection: NavigationAppSelection = NavigationAppSelection.InAppTruckie,
        optimisedPath: RoutePath? = null,
    ): Result {
        if (!RouteNavigateCatalog.isEnabled(flags)) return Result.FlagOff
        if (!NavigateMultiStopCatalog.isEnabled(flags)) return Result.FlagOff
        if (route == null || !route.hasEndpoints) return Result.MissingEndpoints

        val composed = ActiveTourPath.compose(
            route = route,
            vehicle = vehicle,
            bufferM = bufferM,
            composer = composerProvider(),
            optimisedPath = optimisedPath,
        )
        val path = when (composed) {
            is ActiveTourPath.Result.Ready -> composed.path
            is ActiveTourPath.Result.MissingEndpoints -> return Result.MissingEndpoints
            is ActiveTourPath.Result.MissingVehicle ->
                return Result.Blocked("Active vehicle required for constrained Navigate")
            is ActiveTourPath.Result.Blocked -> return Result.Blocked(composed.reason)
        }
        activePath = path

        val end = requireNotNull(route.end)
        val handoff = if (!selection.isInApp) {
            ExternalHandoff(
                packageName = selection.packageName,
                label = selection.label,
                destination = end,
                waypoints = path.points.map {
                    RouteEndpoint(
                        addressLabel = end.addressLabel,
                        latitude = it.latitude,
                        longitude = it.longitude,
                    )
                },
            )
        } else {
            null
        }
        return Result.Ready(path = path, externalHandoff = handoff)
    }
}
