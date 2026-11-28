package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Settings safety disclaimer for Play / select-user release (`ff_settings_disclaimer`, ATNA-96).
 *
 * Defaults **on** (production-safe): Truckie is not a permit; physical roadside
 * signs override digital packs; verify with NHVR / road authorities; on-board
 * weight is guidance only (not a hard lock) — aligned with product prefs.
 *
 * Gherkin: features/play_hardening_backup_disclaimer.feature
 */
object SettingsDisclaimerCatalog {
    const val FLAG_KEY: String = FlagKeys.SETTINGS_DISCLAIMER
    const val TEST_TAG: String = "settings_disclaimer"
    const val TITLE: String = "Safety disclaimer"

    /** Ship / select-user default — legal copy must be visible before Play. */
    const val DEFAULT_ENABLED: Boolean = true
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true
    const val showsInSettings: Boolean = true

    const val notAPermit: Boolean = true
    const val roadsideSignsOverridePacks: Boolean = true
    const val weightIsGuidanceOnly: Boolean = true
    /** Stub “disclaimer lands later” copy is removed when this catalog ships. */
    const val keepsLaterMilestoneStub: Boolean = false

    /**
     * Canonical body for unit assertions; UI uses [R.string.settings_disclaimer_body].
     */
    const val DISCLAIMER_BODY: String =
        "Truckie is not a permit. Physical roadside signs override digital " +
            "packs. Always verify restrictions with NHVR or the road authority. " +
            "On-board weight shown in Truckie is guidance only — not a hard lock."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.SETTINGS_DISCLAIMER)

    fun shouldShow(flags: FeatureFlags): Boolean = isEnabled(flags)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.SETTINGS_DISCLAIMER, enabled)
    }
}
