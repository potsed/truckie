package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Visibility contract for home-map bridge markers:
 * flag on → pack seeds → SymbolManager bind; SEQ samples near Brisbane fallback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BridgeMarkersVisibilityTest {

    @Test
    fun debugFlagGatesMarkersTogetherWithMapHome() {
        val off = LocalFeatureFlags(mapOf(FlagKeys.MAP_HOME to true))
        assertFalse(BridgeMarkersCatalog.shouldShow(off, mapHomeEnabled = true))
        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.BRIDGE_MARKERS to true,
            ),
        )
        assertTrue(BridgeMarkersCatalog.shouldShow(on, mapHomeEnabled = true))
        assertTrue(BridgeMarkersCatalog.isEnabled(on))
    }

    @Test
    fun samplePackSeedsOnFirstLoadWithoutRefresh() {
        val context = RuntimeEnvironment.getApplication()
        val dir = java.io.File(context.filesDir, "bridge_vis_${System.nanoTime()}")
        val repo = CachedBridgePackRepository(context = context, cacheDir = dir)
        assertEquals(null, repo.cachedVersion())
        val pack = repo.loadActive()
        assertTrue(pack.bridges.isNotEmpty())
        assertTrue(
            "First launch must surface bridges without a remote refresh",
            pack.bridges.size >= BridgeMarkersVisibility.MIN_SEQ_BRIDGES,
        )
    }

    @Test
    fun sampleBridgesClusterNearBrisbaneFallbackCamera() {
        val context = RuntimeEnvironment.getApplication()
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
        val nearby = BridgeMarkersVisibility.nearBrisbaneFallback(bridges)
        assertTrue(
            "Need ≥${BridgeMarkersVisibility.MIN_SEQ_BRIDGES} SEQ bridges within " +
                "${BridgeMarkersVisibility.SEQ_RADIUS_KM} km of Brisbane fallback, was ${nearby.size}",
            nearby.size >= BridgeMarkersVisibility.MIN_SEQ_BRIDGES,
        )
        nearby.forEach { bridge ->
            val km = BridgeMarkersVisibility.distanceKm(
                MapCameraDefaults.FALLBACK_LATITUDE,
                MapCameraDefaults.FALLBACK_LONGITUDE,
                bridge.latitude,
                bridge.longitude,
            )
            assertTrue("${bridge.id} is ${km}km from CBD", km <= BridgeMarkersVisibility.SEQ_RADIUS_KM)
        }
    }

    @Test
    fun mapHostMustNotReloadStyleEveryUpdate() {
        assertTrue(BridgeMarkersVisibility.loadsStyleOnce)
        assertTrue(BridgeMarkersVisibility.coalescesConcurrentStyleLoads)
        assertTrue(BridgeMarkersVisibility.recreatesManagersOnNewStyle)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenStyleIdentityDrifts)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenLayerMissing)
        assertTrue(BridgeMarkersVisibility.usesAuRoadSignW58)
        assertTrue(BridgeMarkersVisibility.requiresOpenFreeMapCompatibleTextFont)
        assertEquals(
            "Noto Sans Regular",
            BridgeMarkersVisibility.symbolTextFont.single(),
        )
        assertTrue(
            "W5-8 iconSize should stay neighbourhood-readable " +
                "(${MapSymbolBind.IconSize.neighbourhoodReadableRange}), was ${BridgeMarkersVisibility.iconSize}",
            MapSymbolBind.IconSize.isNeighbourhoodReadable(BridgeMarkersVisibility.iconSize),
        )
    }

    @Test
    fun bootstrapVersionAdvancesReseedCachedSample() {
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.1.0-sample",
            ),
        )
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.2.0-sample",
            ),
        )
        assertEquals("0.7.0-sample", CachedBridgePackRepository.BOOTSTRAP_VERSION)
    }
}
