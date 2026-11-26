package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Demo mode (`ff_demo_mode`, ATNA-56) — Settings toggle for dogfood affordances.
 *
 * When on: [DemoDataset] (DHL Boronia Rd depot, 6 stops, 3.5 m vehicle) via
 * [DemoModeController] with hard isolation from real vehicles/routes.
 * Floating Jump-to-sample-bridges FAB was removed (ATNA-67).
 *
 * Off by default in production **and** debug — opt-in via Settings only.
 * Gherkin: features/demo_mode.feature
 */
object DemoModeCatalog {
    const val FLAG_KEY: String = FlagKeys.DEMO_MODE
    const val SETTINGS_LABEL: String = "Demo mode"
    const val SETTINGS_TEST_TAG: String = "settings_demo_mode_switch"
    const val showsInSettings: Boolean = true

    /** Fresh installs / LocalFeatureFlags default — always off. */
    const val DEFAULT_ENABLED: Boolean = false

    /**
     * Debug dogfood must **not** auto-enable Demo mode (unlike most other flags).
     * Drivers opt in from Settings.
     */
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = false

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.DEMO_MODE)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.DEMO_MODE, enabled)
    }
}
