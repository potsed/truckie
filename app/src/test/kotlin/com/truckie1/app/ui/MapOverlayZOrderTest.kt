package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Overlay stack order (ATNA-122): stops top, route next, others below —
 * via MapLibre native [org.maplibre.android.maps.Style.addLayerAbove].
 *
 * Gherkin: features/map_overlay_z_order.feature
 */
class MapOverlayZOrderTest {

    @Test
    fun flagKeyAndCatalogDefaults() {
        assertEquals("ff_map_overlay_z_order", FlagKeys.MAP_OVERLAY_Z_ORDER)
        assertEquals(FlagKeys.MAP_OVERLAY_Z_ORDER, MapOverlayZOrderCatalog.FLAG_KEY)
        assertTrue(MapOverlayZOrderCatalog.DEFAULT_ENABLED)
        assertTrue(MapOverlayZOrderCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(MapOverlayZOrderCatalog.usesNativeAddLayerAbove)
        assertFalse(MapOverlayZOrderCatalog.usesAnnotationZIndex)
        val flags = LocalFeatureFlags()
        assertFalse(MapOverlayZOrderCatalog.isEnabled(flags))
        MapOverlayZOrderCatalog.setEnabled(flags, true)
        assertTrue(MapOverlayZOrderCatalog.isEnabled(flags))
    }

    @Test
    fun bottomToTopOrderIsBridgesThenRouteThenStops() {
        assertEquals(
            listOf(
                MapStyleOverlayIds.BRIDGE_LAYER,
                MapStyleOverlayIds.ROUTE_LAYER,
                MapStyleOverlayIds.STOP_LAYER,
            ),
            MapOverlayZOrder.bottomToTop,
        )
        assertEquals(MapStyleOverlayIds.STOP_LAYER, MapOverlayZOrder.topOverlayLayerId)
        assertEquals(MapStyleOverlayIds.BRIDGE_LAYER, MapOverlayZOrder.bottomOverlayLayerId)
    }

    @Test
    fun relativeOrderFiltersAndPreservesStackAmongOverlays() {
        val styleBottomToTop = listOf(
            "background",
            "road",
            MapStyleOverlayIds.BRIDGE_LAYER,
            "mapbox-location-layer",
            MapStyleOverlayIds.ROUTE_LAYER,
            MapStyleOverlayIds.STOP_LAYER,
            "poi-label",
        )
        assertEquals(
            listOf(
                MapStyleOverlayIds.BRIDGE_LAYER,
                MapStyleOverlayIds.ROUTE_LAYER,
                MapStyleOverlayIds.STOP_LAYER,
            ),
            MapOverlayZOrder.overlayIdsInStackOrder(styleBottomToTop),
        )
        assertTrue(MapOverlayZOrder.isCorrectRelativeOrder(styleBottomToTop))
    }

    @Test
    fun detectsWrongOrderWhenBridgesSitAboveStops() {
        val wrong = listOf(
            MapStyleOverlayIds.ROUTE_LAYER,
            MapStyleOverlayIds.STOP_LAYER,
            MapStyleOverlayIds.BRIDGE_LAYER,
        )
        assertFalse(MapOverlayZOrder.isCorrectRelativeOrder(wrong))
        assertTrue(
            MapOverlayZOrder.needsReconcile(wrong),
        )
    }

    @Test
    fun detectsWrongOrderWhenRouteSitsAboveStops() {
        val wrong = listOf(
            MapStyleOverlayIds.BRIDGE_LAYER,
            MapStyleOverlayIds.STOP_LAYER,
            MapStyleOverlayIds.ROUTE_LAYER,
        )
        assertFalse(MapOverlayZOrder.isCorrectRelativeOrder(wrong))
        assertTrue(MapOverlayZOrder.needsReconcile(wrong))
    }

    @Test
    fun correctPartialStackDoesNotNeedReconcile() {
        assertTrue(
            MapOverlayZOrder.isCorrectRelativeOrder(
                listOf(MapStyleOverlayIds.ROUTE_LAYER, MapStyleOverlayIds.STOP_LAYER),
            ),
        )
        assertFalse(
            MapOverlayZOrder.needsReconcile(
                listOf(MapStyleOverlayIds.ROUTE_LAYER, MapStyleOverlayIds.STOP_LAYER),
            ),
        )
        assertTrue(
            MapOverlayZOrder.isCorrectRelativeOrder(
                listOf(MapStyleOverlayIds.BRIDGE_LAYER, MapStyleOverlayIds.STOP_LAYER),
            ),
        )
    }

    @Test
    fun bindDeclaresNativeLayerPlacement() {
        assertTrue(MapStyleOverlayBind.usesNativeOverlayZOrder)
        assertTrue(MapStyleOverlayBind.reconcilesOverlayZOrderAfterBind)
        assertFalse(MapStyleOverlayBind.usesAnnotationZIndex)
    }

    @Test
    fun placementAnchorPrefersLayerBelowThenAbove() {
        assertEquals(
            MapOverlayZOrder.Placement.Above(MapStyleOverlayIds.BRIDGE_LAYER),
            MapOverlayZOrder.placementFor(
                layerId = MapStyleOverlayIds.ROUTE_LAYER,
                presentIds = setOf(MapStyleOverlayIds.BRIDGE_LAYER),
            ),
        )
        assertEquals(
            MapOverlayZOrder.Placement.Below(MapStyleOverlayIds.STOP_LAYER),
            MapOverlayZOrder.placementFor(
                layerId = MapStyleOverlayIds.ROUTE_LAYER,
                presentIds = setOf(MapStyleOverlayIds.STOP_LAYER),
            ),
        )
        assertEquals(
            MapOverlayZOrder.Placement.Above(MapStyleOverlayIds.ROUTE_LAYER),
            MapOverlayZOrder.placementFor(
                layerId = MapStyleOverlayIds.STOP_LAYER,
                presentIds = setOf(
                    MapStyleOverlayIds.BRIDGE_LAYER,
                    MapStyleOverlayIds.ROUTE_LAYER,
                ),
            ),
        )
        assertEquals(
            MapOverlayZOrder.Placement.Top,
            MapOverlayZOrder.placementFor(
                layerId = MapStyleOverlayIds.BRIDGE_LAYER,
                presentIds = emptySet(),
            ),
        )
    }
}
