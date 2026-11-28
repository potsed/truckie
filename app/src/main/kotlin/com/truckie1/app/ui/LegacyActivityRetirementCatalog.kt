package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags

/**
 * Architecture catalog for NavHost-only shell (ATNA-74 / ATNA-92).
 *
 * Deletes multi-Activity redirect hosts, Jump residual, unused DualMode vehicle
 * wrapper, and SafeRouteSelector. Product flags [ff_nav_chrome] / [ff_map_home]
 * still gate chrome bars and map-home content inside [TruckieRootApp].
 *
 * Gherkin: features/legacy_activity_retirement.feature
 */
object LegacyActivityRetirementCatalog {
    /** Only launcher Activity remains. */
    const val soleHostActivitySimpleName: String = "MainActivity"

    /** Screen Activities removed from catalog + manifest. */
    val deletedActivitySimpleNames: Set<String> = setOf(
        "VehicleListActivity",
        "VehicleEditActivity",
        "TripStopsActivity",
        "AddStopActivity",
        "BlockedRouteActivity",
        "SettingsActivity",
    )

    /** MainActivity always hosts [TruckieRootApp] — no Activity-level list Home fork. */
    const val alwaysHostsNavHost: Boolean = true

    /** [PrimaryNavigation.shouldUseChromeHost] is unconditionally true. */
    const val chromeHostGateAlwaysOn: Boolean = true

    /** No [ChromeHostRedirect] / Activity [navigateToPrimaryDestination]. */
    const val deletesChromeHostRedirect: Boolean = true
    const val deletesActivityPrimaryNavigation: Boolean = true

    /** Legacy multi-Activity [TruckieHostedScreen] + [TruckieNavScaffold] gone. */
    const val deletesTruckieHostedScreen: Boolean = true

    /**
     * List-first [HomeScreen] remains as NavHost content when `ff_map_home` is off.
     * There is no parallel Activity-host list Home outside the NavHost.
     */
    const val listHomeOnlyInsideNavHost: Boolean = true

    /** ATNA-92: Jump residual helpers deleted (FAB already gone in ATNA-67). */
    const val deletesJumpToSampleBridges: Boolean = true

    /** ATNA-92: unused DualMode vehicle-only wrapper deleted. */
    const val deletesDualModeVehicleProfileRepository: Boolean = true

    /**
     * ATNA-92: [SafeRouteSelector] deleted. [RouteCandidate] stays for
     * MultiStopTourComposer / TourPathSelector. `ff_bridge_safe_routing` remains
     * for future Custom Model work.
     */
    const val deletesSafeRouteSelector: Boolean = true
    const val keepsRouteCandidateForComposer: Boolean = true

    fun isNavHostOnlyShell(_flags: FeatureFlags): Boolean = alwaysHostsNavHost
}
