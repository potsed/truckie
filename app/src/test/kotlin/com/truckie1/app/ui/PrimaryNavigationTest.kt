package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/settings_home_nav.feature — single primary nav path (ATNA-84).
 */
class PrimaryNavigationTest {

    @Test
    fun singlePathPolicyIsAuthoritative() {
        assertTrue(PrimaryNavigation.singlePath)
        assertTrue(PrimaryNavigation.remountsMapHomeFresh)
        assertEquals("MainActivity", PrimaryNavigation.chromeHostActivitySimpleName)
    }

    @Test
    fun homeFromSettingsRemountsMapHomeWithoutRestoreState() {
        val command = PrimaryNavigation.commandFor(
            destination = TruckiePrimaryDestination.HOME,
            fromRoute = TruckieRoutes.SETTINGS,
            mapHomeEnabled = true,
        )
        assertEquals(TruckieRoutes.HOME, command.route)
        assertEquals(TruckieRoutes.HOME, command.popUpToRoute)
        assertTrue(command.popUpToInclusive)
        assertFalse(command.saveState)
        assertFalse(command.restoreState)
        assertTrue(command.launchSingleTop)
        assertTrue(command.remountsDestinationFresh)
    }

    @Test
    fun homeFromSettingsHidesChromeAndLeavesSettings() {
        val command = PrimaryNavigation.commandFor(
            destination = TruckiePrimaryDestination.HOME,
            fromRoute = TruckieRoutes.SETTINGS,
            mapHomeEnabled = true,
        )
        assertEquals(TruckieRoutes.HOME, command.route)
        assertFalse(
            NavChromeCatalog.showBottomBarForRoute(command.route, mapHomeEnabled = true),
        )
        assertFalse(
            NavChromeCatalog.showTopBarForRoute(command.route, mapHomeEnabled = true),
        )
        assertTrue(
            PrimaryNavigation.leavesRoute(
                fromRoute = TruckieRoutes.SETTINGS,
                command = command,
            ),
        )
        assertTrue(
            PrimaryNavigation.landsOnMapHome(command = command, mapHomeEnabled = true),
        )
    }

    @Test
    fun otherPrimaryTabsMayRestoreState() {
        val toSettings = PrimaryNavigation.commandFor(
            destination = TruckiePrimaryDestination.SETTINGS,
            fromRoute = TruckieRoutes.HOME,
            mapHomeEnabled = true,
        )
        assertEquals(TruckieRoutes.SETTINGS, toSettings.route)
        assertEquals(TruckieRoutes.HOME, toSettings.popUpToRoute)
        assertFalse(toSettings.popUpToInclusive)
        assertTrue(toSettings.saveState)
        assertTrue(toSettings.restoreState)
        assertFalse(toSettings.remountsDestinationFresh)

        val toTrip = PrimaryNavigation.commandFor(
            destination = TruckiePrimaryDestination.PLAN_TRIP,
            fromRoute = TruckieRoutes.SETTINGS,
            mapHomeEnabled = true,
        )
        assertTrue(toTrip.saveState)
        assertTrue(toTrip.restoreState)
        assertFalse(toTrip.remountsDestinationFresh)
    }

    @Test
    fun homeWithoutMapHomeMayRestoreState() {
        val command = PrimaryNavigation.commandFor(
            destination = TruckiePrimaryDestination.HOME,
            fromRoute = TruckieRoutes.SETTINGS,
            mapHomeEnabled = false,
        )
        assertEquals(TruckieRoutes.HOME, command.route)
        assertFalse(command.popUpToInclusive)
        assertTrue(command.saveState)
        assertTrue(command.restoreState)
        assertFalse(command.remountsDestinationFresh)
    }

    @Test
    fun chromeHostGateIncludesMapHome() {
        val mapOnly = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAV_CHROME to false,
                FlagKeys.MAP_HOME to true,
            ),
        )
        assertTrue(PrimaryNavigation.shouldUseChromeHost(mapOnly))

        val chromeOnly = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAV_CHROME to true,
                FlagKeys.MAP_HOME to false,
            ),
        )
        assertTrue(PrimaryNavigation.shouldUseChromeHost(chromeOnly))

        val neither = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAV_CHROME to false,
                FlagKeys.MAP_HOME to false,
            ),
        )
        assertFalse(PrimaryNavigation.shouldUseChromeHost(neither))
    }

    @Test
    fun resolveStartRoutePrefersExtraThenHome() {
        assertEquals(
            TruckieRoutes.SETTINGS,
            PrimaryNavigation.resolveStartRoute(TruckieRoutes.SETTINGS),
        )
        assertEquals(
            TruckieRoutes.HOME,
            PrimaryNavigation.resolveStartRoute(null),
        )
        assertEquals(
            TruckieRoutes.HOME,
            PrimaryNavigation.resolveStartRoute("  "),
        )
    }
}
