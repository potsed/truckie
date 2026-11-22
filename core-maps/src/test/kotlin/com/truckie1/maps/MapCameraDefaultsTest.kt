package com.truckie1.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — AU/Brisbane default camera.
 */
class MapCameraDefaultsTest {

    @Test
    fun defaultCameraCentresOnBrisbaneAustralia() {
        assertEquals(-27.4698, MapCameraDefaults.LATITUDE, 0.01)
        assertEquals(153.0251, MapCameraDefaults.LONGITUDE, 0.01)
        assertTrue(
            "Metro overview zoom expected between 9 and 13, was ${MapCameraDefaults.ZOOM}",
            MapCameraDefaults.ZOOM in 9.0..13.0,
        )
    }

    @Test
    fun demotilesStyleIsConfiguredForMapLibre() {
        assertTrue(MapCameraDefaults.STYLE_URI.contains("maplibre", ignoreCase = true))
    }
}
