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
    fun chromeUsesStatusAndNavBarInsets() {
        assertTrue(NavChromeCatalog.topBarUsesStatusBarInsets)
        assertTrue(NavChromeCatalog.bottomBarUsesNavigationBarInsets)
    }

    @Test
    fun chromeIsGlobalFixedAtSingleActivityRoot() {
        assertTrue(NavChromeCatalog.chromeHostedOnceAtRoot)
        assertTrue(NavChromeCatalog.usesSingleActivityNavHost)
        assertTrue(NavChromeCatalog.headerPinnedAtRoot)
        assertTrue(NavChromeCatalog.footerPinnedAtRoot)
        assertTrue(NavChromeCatalog.contentOnlyScrollsBetweenBars)
        assertEquals("MainActivity", NavChromeCatalog.rootHostActivitySimpleName)
    }

    @Test
    fun chromeIsComposeOnlyNoXmlOrActionBar() {
        assertTrue(NavChromeCatalog.composeOnlyChrome)
        assertFalse(NavChromeCatalog.allowsXmlLayouts)
        assertFalse(NavChromeCatalog.allowsAppCompatActionBar)
        assertFalse(ComposeScreenCatalog.usesActionBar)
    }

    @Test
    fun primaryRoutesMapToTripFirstDestinations() {
        assertEquals(
            TruckiePrimaryDestination.HOME,
            NavChromeCatalog.destinationForRoute(TruckieRoutes.HOME),
        )
        assertEquals(
            TruckiePrimaryDestination.PLAN_TRIP,
            NavChromeCatalog.destinationForRoute(TruckieRoutes.PLAN_TRIP),
        )
        assertEquals(
            TruckiePrimaryDestination.VEHICLES,
            NavChromeCatalog.destinationForRoute(TruckieRoutes.VEHICLES),
        )
        assertEquals(
            TruckiePrimaryDestination.SETTINGS,
            NavChromeCatalog.destinationForRoute(TruckieRoutes.SETTINGS),
        )
        assertEquals(
            TruckieRoutes.HOME,
            NavChromeCatalog.routeFor(TruckiePrimaryDestination.HOME),
        )
        assertEquals(
            TruckieRoutes.PLAN_TRIP,
            NavChromeCatalog.routeFor(TruckiePrimaryDestination.PLAN_TRIP),
        )
    }

    @Test
    fun bottomBarShownOnlyOnPrimaryRootRoutes() {
        assertTrue(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.HOME))
        assertTrue(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.PLAN_TRIP))
        assertTrue(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.VEHICLES))
        assertTrue(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.SETTINGS))
        assertFalse(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.ADD_STOP))
        assertFalse(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.VEHICLE_EDIT))
        assertFalse(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.BLOCKED_ROUTE))
    }

    @Test
    fun mapHomeHidesGlobalChromeOnHomeOnly() {
        assertFalse(NavChromeCatalog.showGlobalChromeForRoute(TruckieRoutes.HOME, true))
        assertTrue(NavChromeCatalog.showGlobalChromeForRoute(TruckieRoutes.SETTINGS, true))
        assertTrue(NavChromeCatalog.showTopBarForRoute(TruckieRoutes.PLAN_TRIP, true))
        assertFalse(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.HOME, true))
        assertTrue(NavChromeCatalog.showBottomBarForRoute(TruckieRoutes.HOME, false))
    }

    @Test
    fun primaryNavigationIsSinglePathSharedWithChromeHost() {
        assertTrue(PrimaryNavigation.singlePath)
        assertEquals(
            NavChromeCatalog.rootHostActivitySimpleName,
            PrimaryNavigation.chromeHostActivitySimpleName,
        )
    }
}
