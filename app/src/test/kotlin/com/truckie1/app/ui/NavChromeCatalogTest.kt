package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: nav_chrome.feature — primary destinations + flag gating for chrome.
 */
class NavChromeCatalogTest {

    @Test
    fun primaryDestinationsAreTripFirstForOwnerOperators() {
        assertEquals(
            listOf(
                TruckiePrimaryDestination.HOME,
                TruckiePrimaryDestination.PLAN_TRIP,
                TruckiePrimaryDestination.VEHICLES,
                TruckiePrimaryDestination.SETTINGS,
            ),
            NavChromeCatalog.primaryDestinations,
        )
        assertEquals(4, NavChromeCatalog.primaryDestinations.size)
        assertEquals(1, NavChromeCatalog.tripDestinationIndex)
        assertEquals(
            TruckiePrimaryDestination.PLAN_TRIP,
            NavChromeCatalog.primaryDestinations[NavChromeCatalog.tripDestinationIndex],
        )
        // Settings stays last — secondary to trip / optimize workflow.
        assertEquals(TruckiePrimaryDestination.SETTINGS, NavChromeCatalog.primaryDestinations.last())
        assertEquals("home", TruckiePrimaryDestination.HOME.id)
        assertEquals("plan_trip", TruckiePrimaryDestination.PLAN_TRIP.id)
        assertEquals("vehicles", TruckiePrimaryDestination.VEHICLES.id)
        assertEquals("settings", TruckiePrimaryDestination.SETTINGS.id)
    }

    @Test
    fun navChromeFlagKeyIsFfNavChrome() {
        assertEquals("ff_nav_chrome", FlagKeys.NAV_CHROME)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NAV_CHROME))
        flags.setEnabled(FlagKeys.NAV_CHROME, true)
        assertTrue(flags.isEnabled(FlagKeys.NAV_CHROME))
    }

    @Test
    fun planTripAndVehiclesStillRequireProductFlags() {
        assertEquals(FlagKeys.MULTI_STOP, NavChromeCatalog.requiredFlag(TruckiePrimaryDestination.PLAN_TRIP))
        assertEquals(FlagKeys.VEHICLE_PROFILES, NavChromeCatalog.requiredFlag(TruckiePrimaryDestination.VEHICLES))
        assertNull(NavChromeCatalog.requiredFlag(TruckiePrimaryDestination.HOME))
        assertNull(NavChromeCatalog.requiredFlag(TruckiePrimaryDestination.SETTINGS))
    }

    @Test
    fun canNavigateRespectsProductFlagsWhenChromeOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAV_CHROME to true,
                FlagKeys.MULTI_STOP to false,
                FlagKeys.VEHICLE_PROFILES to true,
            ),
        )
        assertTrue(NavChromeCatalog.canNavigate(TruckiePrimaryDestination.HOME, flags))
        assertFalse(NavChromeCatalog.canNavigate(TruckiePrimaryDestination.PLAN_TRIP, flags))
        assertTrue(NavChromeCatalog.canNavigate(TruckiePrimaryDestination.VEHICLES, flags))
        assertTrue(NavChromeCatalog.canNavigate(TruckiePrimaryDestination.SETTINGS, flags))
    }

    @Test
    fun activityMapsToSelectedPrimaryDestination() {
        assertEquals(
            TruckiePrimaryDestination.HOME,
            NavChromeCatalog.selectedForActivity("MainActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.PLAN_TRIP,
            NavChromeCatalog.selectedForActivity("TripStopsActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.PLAN_TRIP,
            NavChromeCatalog.selectedForActivity("AddStopActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.PLAN_TRIP,
            NavChromeCatalog.selectedForActivity("BlockedRouteActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.VEHICLES,
            NavChromeCatalog.selectedForActivity("VehicleListActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.VEHICLES,
            NavChromeCatalog.selectedForActivity("VehicleEditActivity"),
        )
        assertEquals(
            TruckiePrimaryDestination.SETTINGS,
            NavChromeCatalog.selectedForActivity("SettingsActivity"),
        )
    }

    @Test
    fun primaryScreensShowBottomBarSecondaryDoNot() {
        assertTrue(NavChromeCatalog.showBottomBarForActivity("MainActivity"))
        assertTrue(NavChromeCatalog.showBottomBarForActivity("TripStopsActivity"))
        assertTrue(NavChromeCatalog.showBottomBarForActivity("VehicleListActivity"))
        assertTrue(NavChromeCatalog.showBottomBarForActivity("SettingsActivity"))
        assertFalse(NavChromeCatalog.showBottomBarForActivity("VehicleEditActivity"))
        assertFalse(NavChromeCatalog.showBottomBarForActivity("AddStopActivity"))
        assertFalse(NavChromeCatalog.showBottomBarForActivity("BlockedRouteActivity"))
    }

    @Test
    fun chromeUsesStatusAndNavBarInsets() {
        assertTrue(NavChromeCatalog.topBarUsesStatusBarInsets)
        assertTrue(NavChromeCatalog.bottomBarUsesNavigationBarInsets)
    }
}
