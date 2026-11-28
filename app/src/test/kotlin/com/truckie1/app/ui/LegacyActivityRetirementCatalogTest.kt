package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/legacy_activity_retirement.feature — ATNA-74 / ATNA-92.
 */
class LegacyActivityRetirementCatalogTest {

    @Test
    fun soleHostIsMainActivityAndLegacyHostsAreDeleted() {
        assertEquals("MainActivity", LegacyActivityRetirementCatalog.soleHostActivitySimpleName)
        assertEquals(
            setOf(
                "VehicleListActivity",
                "VehicleEditActivity",
                "TripStopsActivity",
                "AddStopActivity",
                "BlockedRouteActivity",
                "SettingsActivity",
            ),
            LegacyActivityRetirementCatalog.deletedActivitySimpleNames,
        )
        assertEquals(
            setOf("MainActivity"),
            ComposeScreenCatalog.activitySimpleNames.toSet(),
        )
        LegacyActivityRetirementCatalog.deletedActivitySimpleNames.forEach { name ->
            assertFalse(ComposeScreenCatalog.activitySimpleNames.contains(name))
        }
    }

    @Test
    fun chromeHostIsAlwaysNavHost() {
        assertTrue(LegacyActivityRetirementCatalog.alwaysHostsNavHost)
        assertTrue(LegacyActivityRetirementCatalog.chromeHostGateAlwaysOn)
        assertTrue(LegacyActivityRetirementCatalog.deletesChromeHostRedirect)
        assertTrue(LegacyActivityRetirementCatalog.deletesActivityPrimaryNavigation)
        assertTrue(LegacyActivityRetirementCatalog.deletesTruckieHostedScreen)
        assertTrue(PrimaryNavigation.singlePath)
        assertEquals(
            LegacyActivityRetirementCatalog.soleHostActivitySimpleName,
            PrimaryNavigation.chromeHostActivitySimpleName,
        )

        val neither = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAV_CHROME to false,
                FlagKeys.MAP_HOME to false,
            ),
        )
        assertTrue(PrimaryNavigation.shouldUseChromeHost(neither))
        assertTrue(LegacyActivityRetirementCatalog.isNavHostOnlyShell(neither))
    }

    @Test
    fun listHomeOnlyInsideNavHostWhenMapHomeOff() {
        assertTrue(LegacyActivityRetirementCatalog.listHomeOnlyInsideNavHost)
        assertTrue(MapHomeCatalog.hostedInNavHost)
        assertEquals("MainActivity", MapHomeCatalog.hostActivitySimpleName)
    }

    @Test
    fun jumpDualModeAndSafeRouteSelectorResidualsDeleted() {
        assertTrue(LegacyActivityRetirementCatalog.deletesJumpToSampleBridges)
        assertTrue(LegacyActivityRetirementCatalog.deletesDualModeVehicleProfileRepository)
        assertTrue(LegacyActivityRetirementCatalog.deletesSafeRouteSelector)
        assertTrue(LegacyActivityRetirementCatalog.keepsRouteCandidateForComposer)
        assertFalse(MapHomeCatalog.showsJumpToSampleBridgesFab)
        assertTrue(MapHomeOrchestrationCatalog.deletesLegacyActivities)
    }
}
