package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Style-load gate for MapLibreHost: concurrent AndroidView updates must not
 * call setStyle repeatedly, and SymbolManagers must be recreated when the live
 * style identity drifts (orphaned managers → invisible bridge markers).
 *
 * Gherkin: features/bridge_markers.feature — Markers stay visible across
 * recompositions / after Demo jump.
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
        assertFalse(decision.recreateManagers)
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
            "Must not start a second setStyle — that orphans SymbolManagers",
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
        assertFalse(decision.recreateManagers)
    }

    @Test
    fun styleIdentityMismatchRecreatesManagersOnReuse() {
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = true,
            styleLoading = false,
            hasExistingFullyLoadedStyle = true,
            boundStyleMatchesExisting = false,
        )
        assertEquals(StyleEnsureAction.REUSE_EXISTING, decision.action)
        assertTrue(
            "Orphaned SymbolManager must be dropped when map.style !== boundStyle",
            decision.recreateManagers,
        )
    }

    @Test
    fun managerLayerMissingMeansRecreate() {
        assertTrue(
            MapLibreStyleLifecycle.mustRecreateAnnotationManager(
                hasManager = true,
                styleContainsManagerLayer = false,
            ),
        )
        assertFalse(
            MapLibreStyleLifecycle.mustRecreateAnnotationManager(
                hasManager = true,
                styleContainsManagerLayer = true,
            ),
        )
        assertFalse(
            MapLibreStyleLifecycle.mustRecreateAnnotationManager(
                hasManager = false,
                styleContainsManagerLayer = false,
            ),
        )
    }

    @Test
    fun coalescingContractFlagsDocumented() {
        assertTrue(BridgeMarkersVisibility.coalescesConcurrentStyleLoads)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenStyleIdentityDrifts)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenLayerMissing)
        assertTrue(BridgeMarkersVisibility.loadsStyleOnce)
    }
}
