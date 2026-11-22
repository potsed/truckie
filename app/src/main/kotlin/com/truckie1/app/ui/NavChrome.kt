package com.truckie1.app.ui

import com.truckie1.app.R
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Primary bottom-nav destinations for Truckie1 chrome (`ff_nav_chrome`).
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
