package com.truckie1.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — Brisbane fallback + street basemap style.
 */
class MapCameraDefaultsTest {

    @Test
    fun fallbackCameraCentresOnBrisbaneAustralia() {
        assertEquals(-27.4698, MapCameraDefaults.FALLBACK_LATITUDE, 0.01)
        assertEquals(153.0251, MapCameraDefaults.FALLBACK_LONGITUDE, 0.01)
        assertTrue(
            "Metro overview zoom expected between 9 and 13, was ${MapCameraDefaults.ZOOM}",
            MapCameraDefaults.ZOOM in 9.0..13.0,
        )
    }

    @Test
    fun streetBasemapStyleIsOpenFreeMapLiberty() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/liberty",
            MapCameraDefaults.STYLE_URI,
        )
        assertTrue(MapCameraDefaults.STYLE_SOURCE_LABEL.contains("OpenFreeMap", ignoreCase = true))
        assertFalse(
            "Demotiles has no roads — must not be the default style",
            MapCameraDefaults.STYLE_URI.contains("demotiles", ignoreCase = true),
        )
    }
}
