package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/bridge_markers.feature — ff_bridge_markers on MapLibre home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BridgeMarkersCatalogTest {

    @Test
    fun bridgeMarkersFlagKeyIsFfBridgeMarkers() {
        assertEquals("ff_bridge_markers", FlagKeys.BRIDGE_MARKERS)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.BRIDGE_MARKERS))
        flags.setEnabled(FlagKeys.BRIDGE_MARKERS, true)
        assertTrue(flags.isEnabled(FlagKeys.BRIDGE_MARKERS))
    }

    @Test
    fun catalogIsOffByDefaultAndOnWhenFlagEnabled() {
        val off = LocalFeatureFlags()
        assertFalse(BridgeMarkersCatalog.isEnabled(off))
        assertFalse(BridgeMarkersCatalog.shouldShow(off, mapHomeEnabled = true))
        assertFalse(BridgeMarkersCatalog.shouldShow(
            LocalFeatureFlags(mapOf(FlagKeys.BRIDGE_MARKERS to true)),
            mapHomeEnabled = false,
        ))
        val on = LocalFeatureFlags(mapOf(FlagKeys.BRIDGE_MARKERS to true))
        assertTrue(BridgeMarkersCatalog.isEnabled(on))
        assertTrue(BridgeMarkersCatalog.shouldShow(on, mapHomeEnabled = true))
    }

    @Test
    fun usesStyleSymbolLayerNotAnnotationOrDeprecatedMarkerApi() {
        assertTrue(BridgeMarkersCatalog.usesStyleSymbolLayer)
        assertFalse(BridgeMarkersCatalog.usesSymbolManager)
        assertFalse(BridgeMarkersCatalog.usesDeprecatedMarkerApi)
        assertTrue(BridgeMarkersCatalog.usesAuRoadSignW58)
        assertEquals("truckie_bridge_marker_w5_8", BridgeMarkersCatalog.ICON_ID)
        assertEquals("bridges/au_sample_bridges.json", BridgeMarkersCatalog.ASSET_PATH)
        assertEquals("bridges/AU_road_sign_W5-8.svg", BridgeMarkersCatalog.W5_8_SVG_ASSET)
        assertEquals("AU", BridgeMarkersCatalog.REGION)
    }

    @Test
    fun loadsAustraliaFirstSampleBridgesFromAsset() {
        val context = RuntimeEnvironment.getApplication()
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
        assertTrue("Expected sample AU bridges", bridges.size >= 3)
        bridges.forEach { bridge ->
            assertTrue(bridge.id.isNotBlank())
            assertTrue(bridge.name.isNotBlank())
            assertTrue(bridge.latitude in -44.0..-10.0)
            assertTrue(bridge.longitude in 112.0..154.0)
        }
        assertTrue(
            "At least one fixture should include clearance height",
            bridges.any { it.clearanceHeightM != null },
        )
        assertTrue(
            "At least one fixture may omit clearance height",
            bridges.any { it.clearanceHeightM == null },
        )
    }

    @Test
    fun clearanceLabelFormatsShortMetersWhenHeightPresent() {
        val withHeight = BridgePoint(
            id = "demo-1",
            name = "Sample Bridge",
            latitude = -27.47,
            longitude = 153.03,
            clearanceHeightM = 4.3,
            clearanceStatus = BridgeClearanceStatus.VERIFIED,
        )
        assertEquals("4.3 m", BridgeMarkersCatalog.clearanceLabel(withHeight))
        val without = withHeight.copy(clearanceHeightM = null)
        assertNull(BridgeMarkersCatalog.clearanceLabel(without))
    }

    @Test
    fun parseJsonRejectsMalformedPayloadGracefully() {
        val empty = BridgeMarkersCatalog.parseJson("[]")
        assertTrue(empty.isEmpty())
        val bad = BridgeMarkersCatalog.parseJson("{not-json")
        assertTrue(bad.isEmpty())
        val one = BridgeMarkersCatalog.parseJson(
            """
            [
              {
                "id": "qld-demo",
                "name": "Demo Underpass",
                "lat": -27.5,
                "lng": 153.0,
                "clearanceHeightM": 3.6
              }
            ]
            """.trimIndent(),
        )
        assertEquals(1, one.size)
        assertEquals("qld-demo", one[0].id)
        assertEquals(3.6, one[0].clearanceHeightM!!, 0.001)
    }

    @Test
    fun createsNonEmptyBridgeMarkerBitmap() {
        val bitmap = BridgeMarkersCatalog.createBridgeMarkerBitmap(48)
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(48, bitmap.height)
        assertFalse(bitmap.isRecycled)
    }

    @Test
    fun loadsAuRoadSignW58DrawableBitmap() {
        val context = RuntimeEnvironment.getApplication()
        val bitmap = BridgeMarkersCatalog.createBridgeMarkerBitmap(
            sizePx = BridgeMarkersCatalog.DEFAULT_ICON_PX,
            context = context,
        )
        assertNotNull(bitmap)
        assertEquals(BridgeMarkersCatalog.DEFAULT_ICON_PX, bitmap.width)
        assertEquals(BridgeMarkersCatalog.DEFAULT_ICON_PX, bitmap.height)
        assertFalse(bitmap.isRecycled)
        // W5-8 yellow diamond should have non-transparent pixels.
        var opaque = 0
        for (x in 0 until bitmap.width step 8) {
            for (y in 0 until bitmap.height step 8) {
                if ((bitmap.getPixel(x, y) ushr 24) > 0) opaque++
            }
        }
        assertTrue("Expected opaque W5-8 pixels, got $opaque", opaque > 20)
    }

    @Test
    fun shipsW58SvgAssetForProvenance() {
        val context = RuntimeEnvironment.getApplication()
        val exists = runCatching {
            context.assets.open(BridgeMarkersCatalog.W5_8_SVG_ASSET).use { true }
        }.getOrDefault(false)
        assertTrue(exists)
    }
}
