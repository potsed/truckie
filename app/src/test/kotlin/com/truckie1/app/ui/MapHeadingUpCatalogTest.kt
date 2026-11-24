package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_heading_up.feature — Settings switch for course-up.
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
    fun settingsShowsHeadingUpSwitch() {
        assertTrue(MapHeadingUpCatalog.showsInSettings)
        assertEquals("Orient map to driving direction", MapHeadingUpCatalog.SETTINGS_LABEL)
        assertEquals("settings_map_heading_up_switch", MapHeadingUpCatalog.SETTINGS_TEST_TAG)
    }

    @Test
    fun followUsesHeadingUpOnlyWhenSwitchOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertFalse(MapLocationFollow.isHeadingUpEnabled(flags))
        MapHeadingUpCatalog.setEnabled(flags, enabled = true)
        assertTrue(MapLocationFollow.isHeadingUpEnabled(flags))
        MapHeadingUpCatalog.setEnabled(flags, enabled = false)
        assertFalse(MapLocationFollow.isHeadingUpEnabled(flags))
        assertEquals("TRACKING", MapLocationFollow.followingCameraMode(headingUp = false))
    }
}
