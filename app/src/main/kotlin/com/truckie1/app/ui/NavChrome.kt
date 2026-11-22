package com.truckie1.app.ui

import com.truckie1.app.R
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Primary bottom-nav destinations for Truckie chrome (`ff_nav_chrome`).
 *
 * Order is trip-first for single-owner-operator drivers: Home → Trip (pickups/dropoffs
 * + optimize) → Vehicles → Settings (secondary). Product flags still gate Trip / Vehicles.
 */
enum class TruckiePrimaryDestination(
    val id: String,
    val labelRes: Int,
    val titleRes: Int,
    val contentDescriptionRes: Int? = null,
) {
    HOME("home", R.string.nav_home, R.string.home_title),
    PLAN_TRIP(
        "plan_trip",
        R.string.nav_plan_trip,
        R.string.trip_stops,
        R.string.nav_plan_trip_cd,
    ),
    VEHICLES("vehicles", R.string.nav_vehicles, R.string.vehicle_profiles),
    SETTINGS("settings", R.string.nav_settings, R.string.settings),
}

/**
 * Catalog + policy for Material3 TopAppBar / NavigationBar chrome.
 * Gherkin: features/nav_chrome.feature
 *
 * When [FlagKeys.NAV_CHROME] is on, chrome is a **global fixed** shell: one
 * TopAppBar + NavigationBar at the single-activity NavHost root. Destination
 * content swaps/scrolls in the middle; bars never remount on tab changes.
 */
object NavChromeCatalog {
    /** Trip sits immediately after Home — core job before Vehicles/Settings. */
    val primaryDestinations: List<TruckiePrimaryDestination> =
        listOf(
            TruckiePrimaryDestination.HOME,
            TruckiePrimaryDestination.PLAN_TRIP,
            TruckiePrimaryDestination.VEHICLES,
            TruckiePrimaryDestination.SETTINGS,
        )

    /** Index of the trip tab — asserted so Settings never outranks trip chrome. */
    val tripDestinationIndex: Int = 1

    const val topBarUsesStatusBarInsets: Boolean = true
    const val bottomBarUsesNavigationBarInsets: Boolean = true

    /** Chrome Scaffold lives once at the root — not recreated per screen Activity. */
    const val chromeHostedOnceAtRoot: Boolean = true

    /** Primary tabs use a single-activity Compose NavHost when nav chrome is on. */
    const val usesSingleActivityNavHost: Boolean = true

    const val headerPinnedAtRoot: Boolean = true
    const val footerPinnedAtRoot: Boolean = true
    const val contentOnlyScrollsBetweenBars: Boolean = true

    /**
     * Hard product constraint: Material3 Compose chrome only
     * (Scaffold / TopAppBar / NavigationBar + NavHost). No XML layouts,
     * AppCompat ActionBar, or view-based chrome.
     */
    const val composeOnlyChrome: Boolean = true
    const val allowsXmlLayouts: Boolean = false
    const val allowsAppCompatActionBar: Boolean = false

    const val rootHostActivitySimpleName: String = "MainActivity"
    fun requiredFlag(destination: TruckiePrimaryDestination): String? = when (destination) {
        TruckiePrimaryDestination.PLAN_TRIP -> FlagKeys.MULTI_STOP
        TruckiePrimaryDestination.VEHICLES -> FlagKeys.VEHICLE_PROFILES
        TruckiePrimaryDestination.HOME,
        TruckiePrimaryDestination.SETTINGS,
        -> null
    }

    fun canNavigate(destination: TruckiePrimaryDestination, flags: FeatureFlags): Boolean {
        val required = requiredFlag(destination) ?: return true
        return flags.isEnabled(required)
    }

    fun routeFor(destination: TruckiePrimaryDestination): String = when (destination) {
        TruckiePrimaryDestination.HOME -> TruckieRoutes.HOME
        TruckiePrimaryDestination.PLAN_TRIP -> TruckieRoutes.PLAN_TRIP
        TruckiePrimaryDestination.VEHICLES -> TruckieRoutes.VEHICLES
        TruckiePrimaryDestination.SETTINGS -> TruckieRoutes.SETTINGS
    }

    fun destinationForRoute(route: String?): TruckiePrimaryDestination? {
        val base = TruckieRoutes.baseRoute(route) ?: return null
        return when (base) {
            TruckieRoutes.HOME -> TruckiePrimaryDestination.HOME
            TruckieRoutes.PLAN_TRIP,
            TruckieRoutes.ADD_STOP,
            TruckieRoutes.BLOCKED_ROUTE,
            -> TruckiePrimaryDestination.PLAN_TRIP
            TruckieRoutes.VEHICLES,
            TruckieRoutes.VEHICLE_EDIT,
            -> TruckiePrimaryDestination.VEHICLES
            TruckieRoutes.SETTINGS -> TruckiePrimaryDestination.SETTINGS
            else -> null
        }
    }

    fun showBottomBarForRoute(route: String?): Boolean {
        val base = TruckieRoutes.baseRoute(route) ?: return false
        return base in TruckieRoutes.primaryRoutes
    }

    fun selectedForActivity(activitySimpleName: String): TruckiePrimaryDestination? = when (activitySimpleName) {
        "MainActivity" -> TruckiePrimaryDestination.HOME
        "TripStopsActivity",
        "AddStopActivity",
        "BlockedRouteActivity",
        -> TruckiePrimaryDestination.PLAN_TRIP
        "VehicleListActivity",
        "VehicleEditActivity",
        -> TruckiePrimaryDestination.VEHICLES
        "SettingsActivity" -> TruckiePrimaryDestination.SETTINGS
        else -> null
    }

    fun showBottomBarForActivity(activitySimpleName: String): Boolean = when (activitySimpleName) {
        "MainActivity",
        "TripStopsActivity",
        "VehicleListActivity",
        "SettingsActivity",
        -> true
        else -> false
    }
}

/** NavHost route ids for the single-activity chrome shell (`ff_nav_chrome`). */
object TruckieRoutes {
    const val HOME = "home"
    const val PLAN_TRIP = "plan_trip"
    const val VEHICLES = "vehicles"
    const val SETTINGS = "settings"
    const val ADD_STOP = "add_stop"
    const val VEHICLE_EDIT = "vehicle_edit"
    const val BLOCKED_ROUTE = "blocked_route"

    const val ARG_REGO = "rego"
    const val ARG_REASON = "reason"

    val primaryRoutes: Set<String> = setOf(HOME, PLAN_TRIP, VEHICLES, SETTINGS)

    const val VEHICLE_EDIT_WITH_REGO = "$VEHICLE_EDIT/{$ARG_REGO}"
    const val BLOCKED_ROUTE_WITH_REASON = "$BLOCKED_ROUTE/{$ARG_REASON}"

    fun vehicleEdit(rego: String? = null): String =
        if (rego.isNullOrBlank()) VEHICLE_EDIT else "$VEHICLE_EDIT/$rego"

    fun blockedRoute(reason: String): String =
        "$BLOCKED_ROUTE/${android.net.Uri.encode(reason)}"

    fun baseRoute(route: String?): String? {
        if (route.isNullOrBlank()) return null
        return route.substringBefore('?').substringBefore('/')
    }
}
