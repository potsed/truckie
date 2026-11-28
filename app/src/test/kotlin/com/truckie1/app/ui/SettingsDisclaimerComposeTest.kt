package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/play_hardening_backup_disclaimer.feature — Settings UI (ATNA-96).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsDisclaimerComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclaimerVisibleWhenFlagOnAndStubNoteGone() {
        val flags = LocalFeatureFlags()
        SettingsDisclaimerCatalog.setEnabled(flags, true)

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showDisclaimer = SettingsDisclaimerCatalog.shouldShow(flags),
                )
            }
        }

        composeRule.onNodeWithTag(SettingsDisclaimerCatalog.TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("not a permit", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "disclaimer land in later milestones",
            substring = true,
            ignoreCase = true,
        ).assertDoesNotExist()
    }

    @Test
    fun disclaimerHiddenWhenFlagOff() {
        val flags = LocalFeatureFlags()
        SettingsDisclaimerCatalog.setEnabled(flags, false)

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showDisclaimer = SettingsDisclaimerCatalog.shouldShow(flags),
                )
            }
        }

        composeRule.onNodeWithTag(SettingsDisclaimerCatalog.TEST_TAG).assertDoesNotExist()
    }
}
