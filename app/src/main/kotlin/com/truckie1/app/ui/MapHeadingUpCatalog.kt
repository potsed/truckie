package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Map heading-up / course-up (`ff_map_heading_up`) — Settings switch so the
 * driver can orient the map to the direction of travel while following.
 *
 * Off = north-up [MapLocationFollow.FOLLOWING_CAMERA_MODE] (`TRACKING`).
 * On = MapLibre `TRACKING_GPS` + GPS puck ([MapLocationFollow]).
 *
 * Gherkin: features/map_heading_up.feature
 */
object MapHeadingUpCatalog {
    const val FLAG_KEY: String = FlagKeys.MAP_HEADING_UP
    const val SETTINGS_LABEL: String = "Orient map to driving direction"
    const val SETTINGS_TEST_TAG: String = "settings_map_heading_up_switch"
    const val showsInSettings: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.MAP_HEADING_UP)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.MAP_HEADING_UP, enabled)
    }
}
