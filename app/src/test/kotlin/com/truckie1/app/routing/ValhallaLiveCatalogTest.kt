package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/route_follow_roads.feature — Valhalla live catalog. */
class ValhallaLiveCatalogTest {

    @Test
    fun flagKeyAndSelectUserDefaults() {
        assertEquals("ff_valhalla_live", FlagKeys.VALHALLA_LIVE)
        assertEquals(FlagKeys.VALHALLA_LIVE, ValhallaLiveCatalog.FLAG_KEY)
        assertTrue(ValhallaLiveCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertFalse(ValhallaLiveCatalog.DEFAULT_ENABLED)
        assertTrue(ValhallaLiveCatalog.prefersLivePathRouterForDetours)
        assertTrue(ValhallaLiveCatalog.stubIsDegradedFallbackOnly)
        assertEquals(
            "https://api.simplerouting.io/valhalla",
            ValhallaLiveCatalog.SIMPLE_ROUTING_DEFAULT_BASE_URL,
        )
    }

    @Test
    fun missingCredentialsDocumentDegradedStub() {
        val flags = LocalFeatureFlags()
        flags.setEnabled(FlagKeys.VALHALLA_LIVE, true)
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        if (ValhallaLiveCatalog.followsRoadNetwork(flags)) return
        assertTrue(ValhallaLiveCatalog.isDegradedStubActive(flags))
        val missing = ValhallaLiveCatalog.missingForLiveRouting(flags)
        assertTrue(missing.contains("VALHALLA_API_KEY") || missing.contains("VALHALLA_BASE_URL"))
        val status = ValhallaLiveCatalog.routingGeometryStatus(flags)
        assertTrue(status.contains("degraded", ignoreCase = true) || status.contains("stub", ignoreCase = true))
    }

    @Test
    fun selfHostedSwapStepsDocumentCutover() {
        val steps = ValhallaLiveCatalog.selfHostedSwapSteps()
        assertTrue(steps.size >= 3)
        assertTrue(steps.any { it.contains("tiles", ignoreCase = true) })
        assertTrue(steps.any { it.contains("VALHALLA_BASE_URL") })
        assertTrue(steps.any { it.contains("VALHALLA_API_KEY") })
        assertTrue(steps.any { it.contains("oci-valhalla") || it.contains("infra/") })
        assertEquals(
            OciValhallaHostCatalog.STACK_PATH,
            ValhallaLiveCatalog.ociHostStackPath,
        )
    }
}
