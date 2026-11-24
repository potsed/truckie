package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Map heading-up / course-up (`ff_map_heading_up`) — map-chrome orientation
 * lock (same overlay pattern as Recenter), not a Settings switch.
 *
 * Off / unlocked = north-up [MapLocationFollow.FOLLOWING_CAMERA_MODE] (`TRACKING`).
 * On / locked = MapLibre `TRACKING_GPS` + GPS puck ([MapLocationFollow]).
 *
 * Gherkin: features/map_heading_up.feature
 */
object MapHeadingUpCatalog {
    const val FLAG_KEY: String = FlagKeys.MAP_HEADING_UP
    const val MAP_CONTROL_LABEL: String = "Lock map to driving direction"
    const val MAP_CONTROL_TEST_TAG: String = "map_orientation_lock_button"
    const val showsInSettings: Boolean = false
    const val showsOnMapChrome: Boolean = true

    /** @deprecated Prefer [MAP_CONTROL_LABEL]; Settings switch was removed. */
    const val SETTINGS_LABEL: String = MAP_CONTROL_LABEL

    /** @deprecated Prefer [MAP_CONTROL_TEST_TAG]. */
    const val SETTINGS_TEST_TAG: String = "settings_map_heading_up_switch"

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.MAP_HEADING_UP)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.MAP_HEADING_UP, enabled)
    }

    /** Show lock control when follow chrome is available (same gates as Recenter). */
    fun shouldShowOnMap(flags: FeatureFlags): Boolean =
        showsOnMapChrome && MapLocationFollow.isEnabled(flags)

    fun toggle(flags: LocalFeatureFlags): Boolean {
        val next = !isEnabled(flags)
        setEnabled(flags, enabled = next)
        return next
    }
}
