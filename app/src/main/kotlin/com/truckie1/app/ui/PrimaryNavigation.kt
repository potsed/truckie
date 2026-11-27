package com.truckie1.app.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Immutable NavHost options for one primary-tab change.
 * Built by [PrimaryNavigation] — the single path for Home / Trip / Vehicles / Settings.
 */
data class PrimaryNavCommand(
    val route: String,
    val popUpToRoute: String,
    val popUpToInclusive: Boolean,
    val saveState: Boolean,
    val restoreState: Boolean,
    val launchSingleTop: Boolean = true,
) {
    /** MapLibre AndroidView must remount; restoreState left a dead map under hidden chrome. */
    val remountsDestinationFresh: Boolean
        get() = popUpToInclusive && !saveState && !restoreState
}

/**
 * Single primary-tab navigation policy for Truckie (`ff_nav_chrome` / `ff_map_home`).
 *
 * Replaces the previous spaghetti of NavHost `navigatePrimary`, Activity
 * `navigateToPrimaryDestination`, `ChromeHostRedirect`, and inconsistent
 * MainActivity chrome gates.
 *
 * Gherkin: features/settings_home_nav.feature
 */
object PrimaryNavigation {
    const val singlePath: Boolean = true
    const val remountsMapHomeFresh: Boolean = true
    const val chromeHostActivitySimpleName: String = "MainActivity"

    /**
     * Host the Compose NavHost when either global chrome or map-first home is on.
     * Map home requires the single-activity shell even if `ff_nav_chrome` is off.
     */
    fun shouldUseChromeHost(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NAV_CHROME) || flags.isEnabled(FlagKeys.MAP_HOME)

    fun resolveStartRoute(extraRoute: String?): String {
        val trimmed = extraRoute?.trim().orEmpty()
        return trimmed.ifBlank { TruckieRoutes.HOME }
    }

    fun graphStartDestination(startRoute: String): String {
        return when (TruckieRoutes.baseRoute(startRoute)) {
            TruckieRoutes.PLAN_TRIP,
            TruckieRoutes.ADD_STOP,
            TruckieRoutes.BLOCKED_ROUTE,
            -> TruckieRoutes.PLAN_TRIP
            TruckieRoutes.VEHICLES,
            TruckieRoutes.VEHICLE_EDIT,
            -> TruckieRoutes.VEHICLES
            TruckieRoutes.SETTINGS -> TruckieRoutes.SETTINGS
            else -> TruckieRoutes.HOME
        }
    }

    /**
     * Build NavHost options for a primary tab tap.
     *
     * When [mapHomeEnabled] and destination is Home, pop the start destination
     * **inclusive** and skip save/restore so MapLibre remounts. Other tabs keep
     * the standard bottom-nav save/restore pattern.
     */
    fun commandFor(
        destination: TruckiePrimaryDestination,
        fromRoute: String? = null,
        mapHomeEnabled: Boolean = false,
        startRoute: String = TruckieRoutes.HOME,
    ): PrimaryNavCommand {
        val route = NavChromeCatalog.routeFor(destination)
        val popUpTo = graphStartDestination(startRoute)
        val returningToMapHome =
            mapHomeEnabled && destination == TruckiePrimaryDestination.HOME
        // fromRoute documents the Settings → Home edge for callers/tests; policy
        // keys off destination + mapHomeEnabled so Home always remounts fresh.
        check(fromRoute == null || TruckieRoutes.baseRoute(fromRoute) != null) {
            "fromRoute must be a known Truckie route when provided"
        }
        return PrimaryNavCommand(
            route = route,
            popUpToRoute = popUpTo,
            popUpToInclusive = returningToMapHome,
            saveState = !returningToMapHome,
            restoreState = !returningToMapHome,
            launchSingleTop = true,
        )
    }

    fun landsOnMapHome(command: PrimaryNavCommand, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled &&
            TruckieRoutes.baseRoute(command.route) == TruckieRoutes.HOME &&
            command.remountsDestinationFresh

    fun leavesRoute(fromRoute: String, command: PrimaryNavCommand): Boolean {
        val from = TruckieRoutes.baseRoute(fromRoute) ?: return true
        val to = TruckieRoutes.baseRoute(command.route) ?: return true
        return from != to
    }

    /**
     * Apply [command] on [navController]. Prefer route-based popUpTo so tests and
     * runtime share the same policy object (not a one-off NavOptions lambda).
     */
    fun navigate(navController: NavHostController, command: PrimaryNavCommand) {
        val popUpToId = runCatching {
            navController.graph.findNode(command.popUpToRoute)?.id
        }.getOrNull() ?: navController.graph.findStartDestination().id

        navController.navigate(command.route) {
            popUpTo(popUpToId) {
                inclusive = command.popUpToInclusive
                saveState = command.saveState
            }
            launchSingleTop = command.launchSingleTop
            restoreState = command.restoreState
        }
    }

    fun navigatePrimary(
        navController: NavHostController,
        destination: TruckiePrimaryDestination,
        mapHomeEnabled: Boolean,
        currentRoute: String? = null,
        startRoute: String = TruckieRoutes.HOME,
    ) {
        val command = commandFor(
            destination = destination,
            fromRoute = currentRoute,
            mapHomeEnabled = mapHomeEnabled,
            startRoute = startRoute,
        )
        navigate(navController, command)
    }
}
