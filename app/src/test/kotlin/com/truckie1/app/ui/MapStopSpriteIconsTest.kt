package com.truckie1.app.ui

import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Numbered stop sprites on one symbol layer (ATNA-121).
 *
 * Doubled/scribbled stop icons came from textField overlapping a chevron bitmap
 * (and dual bind paths). Contract: MapLibre native style images + icon-only
 * SymbolLayer; order index baked into the sprite; one GeoJSON source / layer.
 *
 * Gherkin: features/map_stop_markers.feature,
 * features/map_style_overlays.feature
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapStopSpriteIconsTest {

    @Test
    fun catalogDeclaresNumberedSpritesWithoutStopTextField() {
        assertTrue(MapStopMarkersCatalog.usesNumberedSpriteIcons)
        assertTrue(MapStopMarkersCatalog.usesStyleSymbolLayer)
        assertFalse(MapStopMarkersCatalog.stopSymbolLayerUsesTextField)
        assertFalse(MapStopMarkersCatalog.usesSymbolManager)
        assertFalse(MapStopMarkersCatalog.usesDeprecatedMarkerApi)
        assertTrue(MapStyleOverlayBind.stopSymbolLayerUsesIconOnly)
        assertFalse(MapStyleOverlayBind.stopSymbolLayerUsesTextField)
        assertTrue(MapLibreHost.usesSingleApplyOverlaysPath)
        assertFalse(MapLibreHost.usesDuplicateOverlayBinds)
    }

    @Test
    fun iconIdsEncodeKindAndOneBasedOrderIndex() {
        assertEquals(
            "truckie_stop_pickup_1",
            MapStopMarkersCatalog.iconIdFor(StopKind.PICKUP_LOAD, orderIndex = 1),
        )
        assertEquals(
            "truckie_stop_dropoff_2",
            MapStopMarkersCatalog.iconIdFor(StopKind.DROP_OFF_UNLOAD, orderIndex = 2),
        )
        assertEquals(
            "truckie_stop_pickup_3",
            MapStopMarkersCatalog.iconIdFor(StopKind.PICKUP_LOAD, orderIndex = 3),
        )
    }

    @Test
    fun geoJsonUsesNumberedSpriteIconsWithoutLabelProperty() {
        val stops = listOf(
            RouteStop(
                id = "s1",
                addressLabel = "Pickup A",
                latitude = -27.47,
                longitude = 153.02,
                kind = StopKind.PICKUP_LOAD,
            ),
            RouteStop(
                id = "s2",
                addressLabel = "Drop B",
                latitude = -27.48,
                longitude = 153.03,
                kind = StopKind.DROP_OFF_UNLOAD,
            ),
        )
        val root = JSONObject(MapOverlayGeoJson.stops(stops))
        val features = root.getJSONArray("features")
        assertEquals(2, features.length())

        val pickup = features.getJSONObject(0).getJSONObject("properties")
        assertEquals("truckie_stop_pickup_1", pickup.getString(MapOverlayGeoJson.PROP_ICON))
        assertFalse(pickup.has(MapOverlayGeoJson.PROP_LABEL))

        val drop = features.getJSONObject(1).getJSONObject("properties")
        assertEquals("truckie_stop_dropoff_2", drop.getString(MapOverlayGeoJson.PROP_ICON))
        assertFalse(drop.has(MapOverlayGeoJson.PROP_LABEL))
    }

    @Test
    fun numberedSpriteBitmapsAreCreatedPerKindAndIndex() {
        val first = MapStopMarkersCatalog.createStopMarkerBitmap(
            kind = StopKind.PICKUP_LOAD,
            orderIndex = 1,
        )
        val second = MapStopMarkersCatalog.createStopMarkerBitmap(
            kind = StopKind.DROP_OFF_UNLOAD,
            orderIndex = 2,
        )
        assertEquals(MapStopMarkersCatalog.DEFAULT_ICON_PX, first.width)
        assertEquals(MapStopMarkersCatalog.DEFAULT_ICON_PX, second.width)
        assertFalse(first.isRecycled)
        assertFalse(second.isRecycled)
    }

    @Test
    fun exactlyOneStopSourceAndLayerId() {
        assertEquals("truckie-stops-source", MapStyleOverlayIds.STOP_SOURCE)
        assertEquals("truckie-stops-symbol", MapStyleOverlayIds.STOP_LAYER)
        assertEquals(1, MapStyleOverlayCatalog.stopOverlaySourceCount)
        assertEquals(1, MapStyleOverlayCatalog.stopOverlaySymbolLayerCount)
    }
}
