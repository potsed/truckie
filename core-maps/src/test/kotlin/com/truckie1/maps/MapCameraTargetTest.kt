package com.truckie1.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — camera prefers current location; Brisbane fallback.
 */
class MapCameraTargetTest {

    @Test
    fun prefersCurrentLocationWhenAvailable() {
        val target = MapCameraTarget.resolve(
            latitude = -33.8688,
            longitude = 151.2093,
        )
        assertEquals(-33.8688, target.latitude, 0.0001)
        assertEquals(151.2093, target.longitude, 0.0001)
        assertFalse(target.usedFallback)
        assertEquals(MapCameraDefaults.ZOOM, target.zoom, 0.0)
    }

    @Test
    fun fallsBackToBrisbaneWhenLocationMissing() {
        val target = MapCameraTarget.resolve(latitude = null, longitude = null)
        assertEquals(MapCameraDefaults.FALLBACK_LATITUDE, target.latitude, 0.0)
        assertEquals(MapCameraDefaults.FALLBACK_LONGITUDE, target.longitude, 0.0)
        assertTrue(target.usedFallback)
    }

    @Test
    fun fallsBackWhenOnlyOneCoordinatePresent() {
        assertTrue(MapCameraTarget.resolve(latitude = -27.0, longitude = null).usedFallback)
        assertTrue(MapCameraTarget.resolve(latitude = null, longitude = 153.0).usedFallback)
    }
}
