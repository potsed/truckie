package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_home_orchestration.feature — ATNA-90 / P1-5.
 */
class MapHomeOrchestrationCatalogTest {

    @Test
    fun productFlagAndArchitectureContracts() {
        assertEquals(FlagKeys.MAP_HOME, MapHomeOrchestrationCatalog.PRODUCT_FLAG_KEY)
        assertTrue(MapHomeOrchestrationCatalog.usesMapHomeController)
        assertTrue(MapHomeOrchestrationCatalog.mapHomeRouteIsThinBinder)
        assertTrue(MapHomeOrchestrationCatalog.deletesInlineMapHomeRouteDomain)
        assertTrue(MapHomeOrchestrationCatalog.mapLibreHostIsDedicatedType)
        assertTrue(MapHomeOrchestrationCatalog.followUsesReconcileThenHost)
        assertTrue(MapHomeOrchestrationCatalog.hostBindsPrecomputedSnapshot)
        assertTrue(MapHomeOrchestrationCatalog.hostDoesNotLookupAppFlags)
        assertTrue(MapHomeOrchestrationCatalog.routeSheetModulesExtracted)
        assertTrue(MapHomeOrchestrationCatalog.rootNavDestinationsSplit)
        assertTrue(MapHomeOrchestrationCatalog.deletesLegacyActivities)
    }

    @Test
    fun enabledOnlyWhenMapHomeOn() {
        val flags = LocalFeatureFlags()
        assertFalse(MapHomeOrchestrationCatalog.isEnabled(flags))
        flags.setEnabled(FlagKeys.MAP_HOME, true)
        assertTrue(MapHomeOrchestrationCatalog.isEnabled(flags))
    }
}
