package com.truckie1.app.ui

import com.truckie1.routing.LatLngPoint
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
 * GeoJSON FeatureCollection builders for style-layer overlays (ATNA-117).
 * Locks Annotation-era visual parity: coordinates, icons, clearance/index labels.
 *
 * Gherkin: features/map_style_overlays.feature, features/bridge_markers.feature,
 * features/map_stop_markers.feature, features/map_route_polyline.feature.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapOverlayGeoJsonTest {

    private val story = BridgePoint(
        id = "qld-story-bridge-sample",
        name = "Story Bridge",
        latitude = -27.4686,
        longitude = 153.0358,
        clearanceHeightM = 3.8,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        passage = BridgePassage.BOTH,
    )

    @Test
    fun bridgesFeatureCollectionMatchesSymbolOptionsParity() {
        val specs = listOf(
            BridgeSymbolSpec(bridge = story, clearanceLabel = "3.8 m"),
            BridgeSymbolSpec(
                bridge = story.copy(
                    id = "qld-captain-cook-bridge-sample",
                    name = "Captain Cook",
                    clearanceHeightM = null,
                ),
                clearanceLabel = null,
            ),
        )
        val root = JSONObject(MapOverlayGeoJson.bridges(specs))
        assertEquals("FeatureCollection", root.getString("type"))
        val features = root.getJSONArray("features")
        assertEquals(2, features.length())

        val labelled = features.getJSONObject(0)
        assertEquals("Feature", labelled.getString("type"))
        assertEquals("Point", labelled.getJSONObject("geometry").getString("type"))
        val coords = labelled.getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(153.0358, coords.getDouble(0), 1e-6)
        assertEquals(-27.4686, coords.getDouble(1), 1e-6)
        val props = labelled.getJSONObject("properties")
        assertEquals("qld-story-bridge-sample", props.getString("id"))
        assertEquals("Story Bridge", props.getString("name"))
        assertEquals("3.8 m", props.getString(MapOverlayGeoJson.PROP_LABEL))
        assertEquals(
            BridgeMarkersCatalog.ICON_ID,
            props.getString(MapOverlayGeoJson.PROP_ICON),
        )

        val unlabelled = features.getJSONObject(1).getJSONObject("properties")
        assertFalse(unlabelled.has(MapOverlayGeoJson.PROP_LABEL))
        assertEquals(
            BridgeMarkersCatalog.ICON_ID,
            unlabelled.getString(MapOverlayGeoJson.PROP_ICON),
        )
    }

    @Test
    fun emptyBridgesYieldEmptyFeatureCollection() {
        val root = JSONObject(MapOverlayGeoJson.bridges(emptyList()))
        assertEquals("FeatureCollection", root.getString("type"))
        assertEquals(0, root.getJSONArray("features").length())
    }

    @Test
    fun stopsFeatureCollectionMatchesSymbolOptionsParity() {
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
            RouteStop(
                id = "missing",
                addressLabel = "No coords",
                kind = StopKind.PICKUP_LOAD,
            ),
        )
        val root = JSONObject(MapOverlayGeoJson.stops(stops))
        val features = root.getJSONArray("features")
        assertEquals(2, features.length())

        val pickup = features.getJSONObject(0)
        val pickupProps = pickup.getJSONObject("properties")
        assertEquals("s1", pickupProps.getString("id"))
        assertEquals(
            MapStopMarkersCatalog.iconIdFor(StopKind.PICKUP_LOAD, 1),
            pickupProps.getString(MapOverlayGeoJson.PROP_ICON),
        )
        // Number baked into sprite — no textField label property (ATNA-121).
        assertFalse(pickupProps.has(MapOverlayGeoJson.PROP_LABEL))
        val pickupCoords = pickup.getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(153.02, pickupCoords.getDouble(0), 1e-6)
        assertEquals(-27.47, pickupCoords.getDouble(1), 1e-6)

        val drop = features.getJSONObject(1).getJSONObject("properties")
        assertEquals(
            MapStopMarkersCatalog.iconIdFor(StopKind.DROP_OFF_UNLOAD, 2),
            drop.getString(MapOverlayGeoJson.PROP_ICON),
        )
        assertFalse(drop.has(MapOverlayGeoJson.PROP_LABEL))
    }

    @Test
    fun routeLineStringMatchesLineManagerParity() {
        val points = listOf(
            LatLngPoint(-27.47, 153.02),
            LatLngPoint(-27.48, 153.03),
            LatLngPoint(-27.49, 153.04),
        )
        val root = JSONObject(MapOverlayGeoJson.routeLine(points))
        assertEquals("FeatureCollection", root.getString("type"))
        val feature = root.getJSONArray("features").getJSONObject(0)
        assertEquals("LineString", feature.getJSONObject("geometry").getString("type"))
        val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
        assertEquals(3, coords.length())
        assertEquals(153.02, coords.getJSONArray(0).getDouble(0), 1e-6)
        assertEquals(-27.47, coords.getJSONArray(0).getDouble(1), 1e-6)
        assertEquals(153.04, coords.getJSONArray(2).getDouble(0), 1e-6)
        assertEquals(-27.49, coords.getJSONArray(2).getDouble(1), 1e-6)
    }

    @Test
    fun routeWithFewerThanTwoPointsIsEmptyFeatureCollection() {
        val one = JSONObject(MapOverlayGeoJson.routeLine(listOf(LatLngPoint(-27.47, 153.02))))
        assertEquals(0, one.getJSONArray("features").length())
        val none = JSONObject(MapOverlayGeoJson.routeLine(emptyList()))
        assertEquals(0, none.getJSONArray("features").length())
    }

    @Test
    fun overlayIdsAreStableAndDistinct() {
        assertTrue(MapStyleOverlayIds.BRIDGE_SOURCE.isNotBlank())
        assertTrue(MapStyleOverlayIds.BRIDGE_LAYER.isNotBlank())
        assertTrue(MapStyleOverlayIds.STOP_SOURCE.isNotBlank())
        assertTrue(MapStyleOverlayIds.STOP_LAYER.isNotBlank())
        assertTrue(MapStyleOverlayIds.ROUTE_SOURCE.isNotBlank())
        assertTrue(MapStyleOverlayIds.ROUTE_LAYER.isNotBlank())
        val ids = listOf(
            MapStyleOverlayIds.BRIDGE_SOURCE,
            MapStyleOverlayIds.BRIDGE_LAYER,
            MapStyleOverlayIds.STOP_SOURCE,
            MapStyleOverlayIds.STOP_LAYER,
            MapStyleOverlayIds.ROUTE_SOURCE,
            MapStyleOverlayIds.ROUTE_LAYER,
        )
        assertEquals(ids.size, ids.toSet().size)
    }
}
