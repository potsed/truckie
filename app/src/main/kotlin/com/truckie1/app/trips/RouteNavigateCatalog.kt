package com.truckie1.app.trips

import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.routing.PathResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.routing.StubGraphHopperPathRouter
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint

/**
 * Catalog for home Navigate (`ff_navigate`).
 *
 * Path finding is **always** in-app GraphHopper (stub until live HTTP) so vehicle
 * constraints can apply. Settings may select an installed nav app for **optional
 * post-route handoff** only — never as a substitute for GraphHopper.
 */
object RouteNavigateCatalog {
    const val FLAG: String = FlagKeys.NAVIGATE

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NAVIGATE)

    fun canNavigate(flags: FeatureFlags, route: Route?): Boolean =
        isEnabled(flags) && route != null && route.hasEndpoints

    fun showsNavigateAction(flags: FeatureFlags): Boolean = isEnabled(flags)

    fun defaultPathRouter(): PathRouter = StubGraphHopperPathRouter()
}

/**
 * Starts Navigate: always computes a GraphHopper path in-app, draws it on MapLibre,
 * then optionally offers external handoff when Settings selects an installed app.
 */
class RouteNavigateSession(
    private val pathRouter: PathRouter = RouteNavigateCatalog.defaultPathRouter(),
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
        selection: NavigationAppSelection = NavigationAppSelection.InAppTruckie,
    ): Result {
        if (!RouteNavigateCatalog.isEnabled(flags)) return Result.FlagOff
        val start = route?.start
        val end = route?.end
        if (start == null || end == null) return Result.MissingEndpoints

        // Always calculate in-app first — external apps never replace GraphHopper.
        val path = when (val pathResult = pathRouter.path(start, end)) {
            is PathResult.Ok -> pathResult.path
            is PathResult.Blocked -> return Result.Blocked(pathResult.reason)
        }
        activePath = path

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
