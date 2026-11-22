package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Demo mode (`ff_demo_mode`) — Settings toggle for temporary dogfood affordances.
 *
 * Today: gates [JumpToSampleBridges] on map-home chrome.
 * Later (not this PR): may include a fuller demo dataset (prefilled routes/data).
 *
 * Gherkin: features/demo_mode.feature
 */
object DemoModeCatalog {
    const val FLAG_KEY: String = FlagKeys.DEMO_MODE
    const val SETTINGS_LABEL: String = "Demo mode"
    const val SETTINGS_TEST_TAG: String = "settings_demo_mode_switch"
    const val showsInSettings: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.DEMO_MODE)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.DEMO_MODE, enabled)
    }
}
