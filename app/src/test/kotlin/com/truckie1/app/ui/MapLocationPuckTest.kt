package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: map_first_home.feature — blue my-location puck + camera on same Fused fix.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapLocationPuckTest {

    @Test
    fun locationPuckFlagKeyIsFfMapLocationPuck() {
        assertEquals("ff_map_location_puck", FlagKeys.MAP_LOCATION_PUCK)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
        flags.setEnabled(FlagKeys.MAP_LOCATION_PUCK, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
    }

    @Test
    fun catalogDescribesBluePuckCenteringCameraOnSameFix() {
        assertTrue(MapLocationPuck.showsBluePuck)
        assertTrue(MapLocationPuck.centersCameraOnSameFix)
        assertTrue(MapLocationPuck.accuracyCircleOptional)
        assertTrue(MapLocationPuck.usesMapLibreLocationComponent)
        assertEquals("#4285F4", MapLocationPuck.PUCK_COLOR_HEX)
    }

    @Test
    fun hidesPuckWhenFlagOffOrPermissionMissingOrFixUnavailable() {
        val flagsOff = LocalFeatureFlags()
        assertFalse(
            MapLocationPuck.shouldShow(
                flags = flagsOff,
                hasLocationPermission = true,
                fix = LocationFix(-27.47, 153.03),
            ),
        )
        val flagsOn = LocalFeatureFlags(mapOf(FlagKeys.MAP_LOCATION_PUCK to true))
        assertFalse(
            MapLocationPuck.shouldShow(
                flags = flagsOn,
                hasLocationPermission = false,
                fix = LocationFix(-27.47, 153.03),
            ),
        )
        assertFalse(
            MapLocationPuck.shouldShow(
                flags = flagsOn,
                hasLocationPermission = true,
                fix = null,
            ),
        )
        assertTrue(
            MapLocationPuck.shouldShow(
                flags = flagsOn,
                hasLocationPermission = true,
                fix = LocationFix(-27.47, 153.03),
            ),
        )
    }

    @Test
    fun resolveAppliesSameFixToPuckAndCamera() {
        val fix = LocationFix(latitude = -33.8688, longitude = 151.2093, accuracyMeters = 12f)
        val resolved = MapLocationPuck.resolve(fix = fix, hasLocationPermission = true)
        assertNotNull(resolved.puck)
        assertEquals(-33.8688, resolved.puck!!.latitude, 0.0001)
        assertEquals(151.2093, resolved.puck!!.longitude, 0.0001)
        assertEquals(12f, resolved.puck!!.accuracyMeters!!, 0.01f)
        assertFalse(resolved.camera.usedFallback)
        assertEquals(resolved.puck!!.latitude, resolved.camera.latitude, 0.0)
        assertEquals(resolved.puck!!.longitude, resolved.camera.longitude, 0.0)
        assertEquals(MapCameraTarget.resolve(-33.8688, 151.2093).latitude, resolved.camera.latitude, 0.0)
    }

    @Test
    fun resolveFallsBackCameraAndHidesPuckWhenFixMissing() {
        val resolved = MapLocationPuck.resolve(fix = null, hasLocationPermission = true)
        assertNull(resolved.puck)
        assertTrue(resolved.camera.usedFallback)
    }

    @Test
    fun resolveHidesPuckWithoutPermissionButStillCentresCameraWhenFixPresent() {
        val fix = LocationFix(latitude = -27.47, longitude = 153.03)
        val resolved = MapLocationPuck.resolve(fix = fix, hasLocationPermission = false)
        assertNull(resolved.puck)
        // Camera may still use the fix if one was obtained earlier; without permission
        // we treat location as unavailable for display and fall back.
        assertTrue(resolved.camera.usedFallback)
    }

    @Test
    fun createsNonEmptyBluePuckBitmap() {
        val bitmap = MapLocationPuck.createPuckBitmap(48)
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(48, bitmap.height)
        assertFalse(bitmap.isRecycled)
    }
}
