package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_heading_up.feature — map orientation lock control.
 */
class MapHeadingUpCatalogTest {

    @Test
    fun headingUpFlagKeyIsFfMapHeadingUp() {
        assertEquals("ff_map_heading_up", FlagKeys.MAP_HEADING_UP)
        assertEquals("ff_map_heading_up", MapHeadingUpCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(MapHeadingUpCatalog.isEnabled(flags))
        MapHeadingUpCatalog.setEnabled(flags, enabled = true)
        assertTrue(MapHeadingUpCatalog.isEnabled(flags))
        MapHeadingUpCatalog.setEnabled(flags, enabled = false)
        assertFalse(MapHeadingUpCatalog.isEnabled(flags))
    }

    @Test
    fun mapChromeShowsOrientationLockNotSettingsSwitch() {
        assertTrue(MapHeadingUpCatalog.showsOnMapChrome)
        assertFalse(MapHeadingUpCatalog.showsInSettings)
        assertEquals("Lock map to driving direction", MapHeadingUpCatalog.MAP_CONTROL_LABEL)
        assertEquals("map_orientation_lock_button", MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG)
    }

    @Test
    fun shouldShowOnMapRequiresFollowFlags() {
        val off = LocalFeatureFlags()
        assertFalse(MapHeadingUpCatalog.shouldShowOnMap(off))
        val follow = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertTrue(MapHeadingUpCatalog.shouldShowOnMap(follow))
    }

    @Test
    fun toggleFlipsHeadingUpFlag() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertFalse(MapLocationFollow.isHeadingUpEnabled(flags))
        assertTrue(MapHeadingUpCatalog.toggle(flags))
        assertTrue(MapLocationFollow.isHeadingUpEnabled(flags))
        assertFalse(MapHeadingUpCatalog.toggle(flags))
        assertFalse(MapLocationFollow.isHeadingUpEnabled(flags))
        assertEquals("TRACKING", MapLocationFollow.followingCameraMode(headingUp = false))
    }
}
