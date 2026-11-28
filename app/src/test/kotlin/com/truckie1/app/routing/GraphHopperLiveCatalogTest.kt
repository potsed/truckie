package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/path_block_detour.feature — live GH select-user wiring.
 */
class GraphHopperLiveCatalogTest {

    @Test
    fun flagKeyAndSelectUserDefaults() {
        assertEquals("ff_graphhopper_live", FlagKeys.GRAPHHOPPER_LIVE)
        assertEquals(FlagKeys.GRAPHHOPPER_LIVE, GraphHopperLiveCatalog.FLAG_KEY)
        assertTrue(GraphHopperLiveCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertFalse(GraphHopperLiveCatalog.DEFAULT_ENABLED)
        assertTrue(GraphHopperLiveCatalog.prefersLivePathRouterForDetours)
        assertTrue(GraphHopperLiveCatalog.stubIsDegradedFallbackOnly)
    }

    @Test
    fun missingForLiveRoutingDocumentsUrlAndFlagGaps() {
        val flags = LocalFeatureFlags()
        val missing = GraphHopperLiveCatalog.missingForLiveRouting(flags)
        assertTrue(missing.contains("ff_graphhopper_live"))
        // BuildConfig URL is empty in unit tests unless property set.
        assertTrue(
            missing.contains("GRAPHHOPPER_BASE_URL") ||
                !GraphHopperLiveCatalog.hasConfiguredBaseUrl(),
        )
        flags.setEnabled(FlagKeys.GRAPHHOPPER_LIVE, true)
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        val after = GraphHopperLiveCatalog.missingForLiveRouting(flags)
        if (!GraphHopperLiveCatalog.hasConfiguredBaseUrl()) {
            assertTrue(after.contains("GRAPHHOPPER_BASE_URL") || after.contains("baseUrl"))
            assertFalse(GraphHopperLiveCatalog.isLivePathRouterActive(flags))
        }
    }
}
