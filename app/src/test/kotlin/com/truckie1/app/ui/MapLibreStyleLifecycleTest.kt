package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Style-load gate for MapLibreHost: concurrent AndroidView updates must not
 * call setStyle repeatedly, and style overlays must be recreated when the live
 * style identity drifts (orphaned layers → invisible bridge markers).
 *
 * Gherkin: features/bridge_markers.feature, features/map_style_overlays.feature
 */
class MapLibreStyleLifecycleTest {

    @Test
    fun firstEnsureStartsSingleStyleLoad() {
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = false,
            styleLoading = false,
            hasExistingFullyLoadedStyle = false,
            boundStyleMatchesExisting = false,
        )
        assertEquals(StyleEnsureAction.START_LOAD, decision.action)
        assertFalse(decision.recreateOverlays)
    }

    @Test
    fun concurrentEnsureWhileLoadingOnlyEnqueues() {
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = false,
            styleLoading = true,
            hasExistingFullyLoadedStyle = false,
            boundStyleMatchesExisting = false,
        )
        assertEquals(StyleEnsureAction.ENQUEUE_WHILE_LOADING, decision.action)
        assertFalse(
            "Must not start a second setStyle — that orphans overlays",
            decision.action == StyleEnsureAction.START_LOAD,
        )
    }

    @Test
    fun readyLiveStyleIsReusedWithoutSetStyle() {
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = true,
            styleLoading = false,
            hasExistingFullyLoadedStyle = true,
            boundStyleMatchesExisting = true,
        )
        assertEquals(StyleEnsureAction.REUSE_EXISTING, decision.action)
        assertFalse(decision.recreateOverlays)
    }

    @Test
    fun styleIdentityMismatchRecreatesOverlaysOnReuse() {
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = true,
            styleLoading = false,
            hasExistingFullyLoadedStyle = true,
            boundStyleMatchesExisting = false,
        )
        assertEquals(StyleEnsureAction.REUSE_EXISTING, decision.action)
        assertTrue(
            "Orphaned overlays must be dropped when map.style !== boundStyle",
            decision.recreateOverlays,
        )
    }

    @Test
    fun overlayLayerMissingMeansRecreate() {
        assertTrue(
            MapLibreStyleLifecycle.mustRecreateOverlay(
                hasBoundOverlay = true,
                styleContainsLayer = false,
            ),
        )
        assertFalse(
            MapLibreStyleLifecycle.mustRecreateOverlay(
                hasBoundOverlay = true,
                styleContainsLayer = true,
            ),
        )
        assertFalse(
            MapLibreStyleLifecycle.mustRecreateOverlay(
                hasBoundOverlay = false,
                styleContainsLayer = false,
            ),
        )
    }

    @Test
    fun coalescingContractFlagsDocumented() {
        assertTrue(BridgeMarkersVisibility.coalescesConcurrentStyleLoads)
        assertTrue(BridgeMarkersVisibility.recreatesOverlaysWhenStyleIdentityDrifts)
        assertTrue(BridgeMarkersVisibility.recreatesOverlaysWhenLayerMissing)
        assertTrue(BridgeMarkersVisibility.loadsStyleOnce)
    }
}
