package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_street_labels.feature — ff_map_street_labels
 * prefers road/street names over bus-stop / clutter POI labels.
 */
class MapStreetLabelPolicyTest {

    @Test
    fun streetLabelsFlagKeyIsFfMapStreetLabels() {
        assertEquals("ff_map_street_labels", FlagKeys.MAP_STREET_LABELS)
        assertEquals(FlagKeys.MAP_STREET_LABELS, MapStreetLabelPolicy.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_STREET_LABELS))
        flags.setEnabled(FlagKeys.MAP_STREET_LABELS, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_STREET_LABELS))
    }

    @Test
    fun catalogIsOffByDefaultAndOnWhenFlagEnabled() {
        val off = LocalFeatureFlags()
        assertFalse(MapStreetLabelPolicy.isEnabled(off))
        assertFalse(MapStreetLabelPolicy.shouldApply(off, mapHomeEnabled = true))
        assertFalse(
            MapStreetLabelPolicy.shouldApply(
                LocalFeatureFlags(mapOf(FlagKeys.MAP_STREET_LABELS to true)),
                mapHomeEnabled = false,
            ),
        )
        val on = LocalFeatureFlags(mapOf(FlagKeys.MAP_STREET_LABELS to true))
        assertTrue(MapStreetLabelPolicy.isEnabled(on))
        assertTrue(MapStreetLabelPolicy.shouldApply(on, mapHomeEnabled = true))
    }

    @Test
    fun keepsOpenFreeMapLibertyStyleUriNotACustomHostedStyle() {
        assertTrue(MapStreetLabelPolicy.keepsOpenFreeMapLibertyStyleUri)
        assertEquals(MapCameraDefaults.STYLE_URI, MapStreetLabelPolicy.styleUri)
        assertEquals(
            "https://tiles.openfreemap.org/styles/liberty",
            MapStreetLabelPolicy.styleUri,
        )
        assertFalse(MapStreetLabelPolicy.usesCustomHostedStyleOverride)
    }

    @Test
    fun usesRuntimeLayerVisibilityAndMinZoomTweaks() {
        assertTrue(MapStreetLabelPolicy.usesRuntimeLayerTweaks)
        assertFalse(MapStreetLabelPolicy.rehostsBasemapTiles)
    }

    @Test
    fun highwayNameLayersAreVisibleAndMinorMinZoomAtOrBelowDrivingZoom() {
        val adjustments = MapStreetLabelPolicy.adjustments()
        val byId = adjustments.associateBy { it.layerId }

        val major = byId.getValue(MapStreetLabelPolicy.HIGHWAY_NAME_MAJOR)
        val minor = byId.getValue(MapStreetLabelPolicy.HIGHWAY_NAME_MINOR)
        assertEquals(LayerVisibilityIntent.VISIBLE, major.visibility)
        assertEquals(LayerVisibilityIntent.VISIBLE, minor.visibility)

        val minorMinZoom = requireNotNull(minor.minZoom) {
            "highway-name-minor must lower minzoom for driving zooms"
        }
        assertTrue(
            "minor minzoom $minorMinZoom must be <= driving zoom ${MapCameraDefaults.ZOOM}",
            minorMinZoom <= MapCameraDefaults.ZOOM,
        )
        assertTrue(
            "minor minzoom should be below Liberty default 15 so more streets show",
            minorMinZoom < 15.0,
        )
    }

    @Test
    fun hidesBusStopTransitAndRankedPoiClutterLayers() {
        val adjustments = MapStreetLabelPolicy.adjustments()
        val byId = adjustments.associateBy { it.layerId }

        MapStreetLabelPolicy.HIDDEN_POI_LAYERS.forEach { id ->
            assertEquals(
                "Expected $id hidden so street names win collision",
                LayerVisibilityIntent.NONE,
                byId.getValue(id).visibility,
            )
        }
        assertTrue(MapStreetLabelPolicy.HIDDEN_POI_LAYERS.contains("poi_transit"))
        assertTrue(MapStreetLabelPolicy.HIDDEN_POI_LAYERS.contains("poi_r1"))
        assertTrue(MapStreetLabelPolicy.HIDDEN_POI_LAYERS.contains("poi_r7"))
        assertTrue(MapStreetLabelPolicy.HIDDEN_POI_LAYERS.contains("poi_r20"))
        assertFalse(
            "Airport IATA labels stay for orientation",
            MapStreetLabelPolicy.HIDDEN_POI_LAYERS.contains("airport"),
        )
    }

    @Test
    fun applyToStyleIsNoOpWhenFlagOff() {
        val recorder = RecordingStyleLayerMutator()
        MapStreetLabelPolicy.applyToStyle(
            mutator = recorder,
            enabled = false,
        )
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun applyToStyleRecordsVisibilityAndMinZoomForKnownLayers() {
        val recorder = RecordingStyleLayerMutator()
        MapStreetLabelPolicy.applyToStyle(
            mutator = recorder,
            enabled = true,
        )
        assertTrue(recorder.calls.isNotEmpty())
        assertTrue(recorder.calls.any { it.startsWith("visibility:highway-name-major:visible") })
        assertTrue(recorder.calls.any { it.startsWith("visibility:highway-name-minor:visible") })
        assertTrue(recorder.calls.any { it.startsWith("visibility:poi_transit:none") })
        assertTrue(recorder.calls.any { it.startsWith("minZoom:highway-name-minor:") })
        // Missing layers must be skipped safely (OpenFreeMap may rename layers later).
        recorder.missingLayers.add("poi_r1")
        recorder.calls.clear()
        MapStreetLabelPolicy.applyToStyle(mutator = recorder, enabled = true)
        assertFalse(recorder.calls.any { it.contains("poi_r1") })
    }

    private class RecordingStyleLayerMutator : StyleLayerMutator {
        val calls = mutableListOf<String>()
        val missingLayers = mutableSetOf<String>()

        override fun hasLayer(layerId: String): Boolean = layerId !in missingLayers

        override fun setVisibility(layerId: String, visible: Boolean) {
            calls += "visibility:$layerId:${if (visible) "visible" else "none"}"
        }

        override fun setMinZoom(layerId: String, minZoom: Double) {
            calls += "minZoom:$layerId:$minZoom"
        }
    }
}
