package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recenter + orientation lock share the same map-chrome icon treatment.
 */
class MapChromeIconControlsTest {

    @Test
    fun consistentVisualTreatmentIsCoreRequirement() {
        assertTrue(MapChromeIconControls.consistentVisualTreatmentRequired)
    }

    @Test
    fun recenterAndOrientationLockSharePrimaryChrome() {
        assertTrue(MapChromeIconControls.usesCircleShape)
        assertTrue(MapChromeIconControls.usesPrimarySurface)
        assertTrue(MapChromeIconControls.usesOnPrimaryIconTint)
        assertEquals(4, MapChromeIconControls.shadowElevationDp)
        assertEquals("map_recenter_button", MapChromeIconControls.RECENTER_TEST_TAG)
        assertEquals(
            MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG,
            "map_orientation_lock_button",
        )
        assertEquals("MyLocation", MapChromeIconControls.RECENTER_ICON)
        assertEquals("Navigation", MapChromeIconControls.ORIENTATION_LOCK_ICON)
        assertEquals("Explore", MapChromeIconControls.ORIENTATION_UNLOCK_ICON)
    }
}
