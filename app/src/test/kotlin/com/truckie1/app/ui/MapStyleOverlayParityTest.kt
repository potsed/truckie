package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Annotation → style-layer parity contracts (ATNA-117).
 *
 * Proves the style-layer path keeps the same visual / filter behaviour the
 * SymbolManager / LineManager path shipped: icon sizes, fonts, colours, widths,
 * shared bind path, no deprecated Marker API, no Annotation plugin.
 *
 * Gherkin: features/map_style_overlays.feature
 */
class MapStyleOverlayParityTest {

    @Test
    fun usesStyleLayersNotAnnotationManagers() {
        assertTrue(MapStyleOverlayCatalog.usesGeoJsonSource)
        assertTrue(MapStyleOverlayCatalog.usesStyleSymbolLayers)
        assertTrue(MapStyleOverlayCatalog.usesStyleLineLayer)
        assertFalse(MapStyleOverlayCatalog.usesAnnotationSymbolManager)
        assertFalse(MapStyleOverlayCatalog.usesAnnotationLineManager)
        assertFalse(MapStyleOverlayCatalog.usesDeprecatedMarkerApi)
        assertTrue(MapStyleOverlayCatalog.sharesOneBindPathForBridgesStopsAndRoute)
        assertTrue(MapStyleOverlayCatalog.usesMapLibreStyleLifecycle)
        assertTrue(MapStyleOverlayCatalog.preservesRecenterAndFollow)
    }

    @Test
    fun bridgeCatalogDeclaresStyleSymbolLayerParity() {
        assertTrue(BridgeMarkersCatalog.usesStyleSymbolLayer)
        assertFalse(BridgeMarkersCatalog.usesSymbolManager)
        assertFalse(BridgeMarkersCatalog.usesDeprecatedMarkerApi)
        assertTrue(BridgeMarkersCatalog.usesAuRoadSignW58)
        assertEquals("truckie_bridge_marker_w5_8", BridgeMarkersCatalog.ICON_ID)
        assertEquals(
            MapStyleOverlayCatalog.IconSize.BRIDGE_W58,
            BridgeMarkersVisibility.iconSize,
        )
        assertEquals(
            MapLibreSymbolText.openFreeMapTextFont.toList(),
            BridgeMarkersVisibility.symbolTextFont.toList(),
        )
        assertTrue(MapLibreSymbolText.textOptional)
        assertEquals("#0B1F33", MapStyleOverlayCatalog.BridgeLabel.TEXT_COLOR_HEX)
        assertEquals("#FFFFFF", MapStyleOverlayCatalog.BridgeLabel.TEXT_HALO_COLOR_HEX)
        assertEquals(12f, MapStyleOverlayCatalog.BridgeLabel.TEXT_SIZE)
        assertEquals(1.4f, MapStyleOverlayCatalog.BridgeLabel.TEXT_HALO_WIDTH)
        assertEquals(
            listOf(0f, -1.6f),
            MapStyleOverlayCatalog.BridgeLabel.TEXT_OFFSET.toList(),
        )
    }

    @Test
    fun stopCatalogDeclaresStyleSymbolLayerParity() {
        assertTrue(MapStopMarkersCatalog.usesStyleSymbolLayer)
        assertTrue(MapStopMarkersCatalog.usesNumberedSpriteIcons)
        assertFalse(MapStopMarkersCatalog.stopSymbolLayerUsesTextField)
        assertFalse(MapStopMarkersCatalog.usesSymbolManager)
        assertFalse(MapStopMarkersCatalog.usesDeprecatedMarkerApi)
        assertTrue(MapStopMarkersCatalog.usesSharedStyleOverlayBindPath)
        assertTrue(MapStopMarkersCatalog.sharesStyleLifecycleWithBridges)
        assertEquals(
            MapStyleOverlayCatalog.IconSize.STOP,
            MapStopMarkersCatalog.ICON_SIZE,
        )
        assertEquals("center", MapStyleOverlayCatalog.StopPin.ICON_ANCHOR)
        assertTrue(MapStyleOverlayBind.stopSymbolLayerUsesIconOnly)
        assertFalse(MapStyleOverlayBind.stopSymbolLayerUsesTextField)
        assertTrue(MapLibreHost.usesSingleApplyOverlaysPath)
        assertFalse(MapLibreHost.usesDuplicateOverlayBinds)
    }

    @Test
    fun routeCatalogDeclaresStyleLineLayerParity() {
        assertTrue(MapRoutePathStyle.usesStyleLineLayer)
        assertFalse(MapRoutePathStyle.usesLineManager)
        assertTrue(MapRoutePathStyle.usesSharedStyleOverlayBindPath)
        assertTrue(MapRoutePolylineCatalog.usesStyleLineLayer)
        assertFalse(MapRoutePolylineCatalog.usesLineManager)
        assertTrue(MapRoutePolylineCatalog.usesSharedStyleOverlayBindPath)
        assertEquals("#0B1F33", MapRoutePathStyle.LINE_COLOR_HEX)
        assertEquals(5.0f, MapRoutePathStyle.LINE_WIDTH)
        assertEquals(MapRoutePathStyle.LINE_COLOR_HEX, MapRoutePolylineCatalog.LINE_COLOR_HEX)
        assertEquals(MapRoutePathStyle.LINE_WIDTH, MapRoutePolylineCatalog.LINE_WIDTH)
    }

    @Test
    fun sharedIconSizePolicyMatchesAnnotationEraDanielScales() {
        assertEquals(1.1f, MapStyleOverlayCatalog.IconSize.STOP_BASELINE)
        assertEquals(1.15f, MapStyleOverlayCatalog.IconSize.BRIDGE_W58_BASELINE)
        assertEquals(1.1f * 1.5f, MapStyleOverlayCatalog.IconSize.STOP)
        // Relative sizing kept: 0.5× baseline, then Daniel dogfood +25% (ATNA-118)
        // and another +25% (ATNA-119) on the style-layer icon-size.
        assertEquals(1.25f, MapStyleOverlayCatalog.IconSize.BRIDGE_W58_ENLARGE)
        assertEquals(
            1.15f * 0.5f * 1.25f * 1.25f,
            MapStyleOverlayCatalog.IconSize.BRIDGE_W58,
        )
        assertTrue(
            MapStyleOverlayCatalog.IconSize.STOP > MapStyleOverlayCatalog.IconSize.BRIDGE_W58,
        )
        assertTrue(
            MapStyleOverlayCatalog.IconSize.isNeighbourhoodReadable(
                MapStyleOverlayCatalog.IconSize.STOP,
            ),
        )
        assertTrue(
            MapStyleOverlayCatalog.IconSize.isNeighbourhoodReadable(
                MapStyleOverlayCatalog.IconSize.BRIDGE_W58,
            ),
        )
    }

    @Test
    fun visibilityLifecycleFlagsSpeakOverlaysNotManagers() {
        assertTrue(BridgeMarkersVisibility.loadsStyleOnce)
        assertTrue(BridgeMarkersVisibility.coalescesConcurrentStyleLoads)
        assertTrue(BridgeMarkersVisibility.recreatesOverlaysOnNewStyle)
        assertTrue(BridgeMarkersVisibility.recreatesOverlaysWhenStyleIdentityDrifts)
        assertTrue(BridgeMarkersVisibility.recreatesOverlaysWhenLayerMissing)
        assertFalse(BridgeMarkersVisibility.recreatesManagersOnNewStyle)
        assertFalse(BridgeMarkersVisibility.recreatesManagersWhenStyleIdentityDrifts)
        assertFalse(BridgeMarkersVisibility.recreatesManagersWhenLayerMissing)
    }

    @Test
    fun pipelineBindReceivesPreFilteredListForStyleLayers() {
        assertTrue(BridgeVisibilityPipeline.singlePipelineOwnsMapFilters)
        assertTrue(BridgeVisibilityPipeline.bindReceivesPreFilteredList)
        val pre = listOf(
            BridgePoint(
                id = "a",
                name = "A",
                latitude = -27.0,
                longitude = 153.0,
            ),
        )
        assertEquals(pre, BridgeVisibilityPipeline.forStyleBind(pre))
    }
}
