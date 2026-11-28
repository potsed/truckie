package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/play_hardening_backup_disclaimer.feature — disclaimer (ATNA-96).
 */
class SettingsDisclaimerCatalogTest {

    @Test
    fun flagKeyAndDefaultsOnForPlaySafety() {
        assertEquals("ff_settings_disclaimer", FlagKeys.SETTINGS_DISCLAIMER)
        assertEquals(FlagKeys.SETTINGS_DISCLAIMER, SettingsDisclaimerCatalog.FLAG_KEY)
        assertTrue(SettingsDisclaimerCatalog.DEFAULT_ENABLED)
        assertTrue(SettingsDisclaimerCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(SettingsDisclaimerCatalog.showsInSettings)
    }

    @Test
    fun flagToggleGatesVisibility() {
        val flags = LocalFeatureFlags()
        // Fresh LocalFeatureFlags is empty — catalog defaults are applied at app start.
        assertFalse(SettingsDisclaimerCatalog.isEnabled(flags))
        assertFalse(SettingsDisclaimerCatalog.shouldShow(flags))

        SettingsDisclaimerCatalog.setEnabled(flags, true)
        assertTrue(SettingsDisclaimerCatalog.isEnabled(flags))
        assertTrue(SettingsDisclaimerCatalog.shouldShow(flags))

        SettingsDisclaimerCatalog.setEnabled(flags, false)
        assertFalse(SettingsDisclaimerCatalog.shouldShow(flags))
    }

    @Test
    fun disclaimerCopyCoversPermitSignsNhvrAndGuidanceWeight() {
        val text = SettingsDisclaimerCatalog.DISCLAIMER_BODY.lowercase()
        assertTrue(text.contains("not a permit"))
        assertTrue(text.contains("roadside") || text.contains("road-side"))
        assertTrue(text.contains("override") || text.contains("overrides"))
        assertTrue(text.contains("nhvr") || text.contains("road authorit"))
        assertTrue(
            text.contains("guidance") || text.contains("guide only") || text.contains("not a hard lock"),
        )
        assertTrue(SettingsDisclaimerCatalog.weightIsGuidanceOnly)
        assertTrue(SettingsDisclaimerCatalog.roadsideSignsOverridePacks)
        assertTrue(SettingsDisclaimerCatalog.notAPermit)
        assertEquals("settings_disclaimer", SettingsDisclaimerCatalog.TEST_TAG)
        assertFalse(SettingsDisclaimerCatalog.keepsLaterMilestoneStub)
    }
}
