package com.truckie1.app.nhvr

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/nhvr_overlay.feature — ff_nhvr_overlay behaviour contracts (ATNA-95).
 */
class NhvrOverlayCatalogTest {

    @Test
    fun flagKeyDefaultsOffIncludingDebug() {
        assertEquals("ff_nhvr_overlay", FlagKeys.NHVR_OVERLAY)
        assertEquals(FlagKeys.NHVR_OVERLAY, NhvrOverlayCatalog.FLAG_KEY)
        assertFalse(NhvrOverlayCatalog.DEFAULT_ENABLED)
        assertFalse(NhvrOverlayCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NHVR_OVERLAY))
        assertFalse(NhvrOverlayCatalog.isEnabled(flags))
    }

    @Test
    fun overlayNotDrawnWhenFlagOff() {
        val flags = LocalFeatureFlags()
        assertFalse(NhvrOverlayCatalog.shouldDrawOverlay(flags))
        assertTrue(NhvrOverlayCatalog.bridgePacksAreSeparateConcern)
    }

    @Test
    fun enablingFlagExposesOverlayContractOnly() {
        val flags = LocalFeatureFlags()
        NhvrOverlayCatalog.setEnabled(flags, true)
        assertTrue(NhvrOverlayCatalog.isEnabled(flags))
        assertTrue(NhvrOverlayCatalog.shouldDrawOverlay(flags))
        assertEquals(
            "https://maps.nhvr.gov.au/",
            NhvrOverlayCatalog.authorityMapUrl,
        )
        assertTrue(NhvrOverlayCatalog.routingRemainsInAppPathRouter)
        assertTrue(NhvrOverlayCatalog.requiresCitedSourceMetadata)
        assertTrue(NhvrOverlayCatalog.neverInventsRestrictionGeometries)
        assertTrue(NhvrOverlayCatalog.roadsideSignsAndPermitsOverrideDigital)
    }

    @Test
    fun overlayDoesNotReplaceBridgePacks() {
        assertFalse(NhvrOverlayCatalog.substitutesForBridgeMarkers)
        assertEquals("ATNA-43", NhvrOverlayCatalog.otherStateIngestTrackedBy)
        assertEquals("ATNA-11", NhvrOverlayCatalog.liveIngestTrackedBy)
    }
}
