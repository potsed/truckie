package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shared SymbolManager obtain / defaults for stop + bridge home-map markers.
 * Gherkin: features/map_stop_markers.feature, features/bridge_markers.feature.
 */
class MapSymbolBindTest {

    @Test
    fun oneSharedPathForStopsAndBridges() {
        assertTrue(MapSymbolBind.sharesPathWithBridgeAndStopMarkers)
        assertTrue(MapSymbolBind.usesMapLibreStyleLifecycle)
        assertTrue(MapSymbolBind.iconAllowOverlap)
        assertTrue(MapSymbolBind.iconIgnorePlacement)
        assertTrue(MapSymbolBind.textAllowOverlap)
        assertEquals(MapLibreSymbolText.textOptional, MapSymbolBind.textOptional)
    }

    @Test
    fun sharedIconSizePolicyAppliesDanielScaleFactors() {
        assertEquals(1.1f, MapSymbolBind.IconSize.STOP_BASELINE)
        assertEquals(1.15f, MapSymbolBind.IconSize.BRIDGE_W58_BASELINE)
        assertEquals(1.1f * 1.5f, MapSymbolBind.IconSize.STOP)
        assertEquals(1.15f * 0.5f, MapSymbolBind.IconSize.BRIDGE_W58)
        assertEquals(MapSymbolBind.IconSize.STOP, MapStopMarkersCatalog.ICON_SIZE)
        assertEquals(MapSymbolBind.IconSize.BRIDGE_W58, BridgeMarkersVisibility.iconSize)
        assertTrue(
            "Stops (${MapSymbolBind.IconSize.STOP}) should read larger than W5-8 " +
                "(${MapSymbolBind.IconSize.BRIDGE_W58})",
            MapSymbolBind.IconSize.STOP > MapSymbolBind.IconSize.BRIDGE_W58,
        )
        assertTrue(MapSymbolBind.IconSize.isNeighbourhoodReadable(MapSymbolBind.IconSize.STOP))
        assertTrue(MapSymbolBind.IconSize.isNeighbourhoodReadable(MapSymbolBind.IconSize.BRIDGE_W58))
    }

    @Test
    fun stopAndBridgeCatalogsDeclareSharedBind() {
        assertTrue(MapStopMarkersCatalog.usesSharedSymbolBindPath)
        assertTrue(MapStopMarkersCatalog.sharesStyleLifecycleWithBridges)
        assertTrue(BridgeMarkersVisibility.loadsStyleOnce)
        assertTrue(BridgeMarkersVisibility.recreatesManagersOnNewStyle)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenStyleIdentityDrifts)
        assertTrue(BridgeMarkersVisibility.recreatesManagersWhenLayerMissing)
    }
}
