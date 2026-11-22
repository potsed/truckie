package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/demo_mode.feature — Settings Demo mode switch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DemoModeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsDemoModeSwitchTogglesFlag() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.DEMO_MODE to true))
        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showDemoModeSwitch = true,
                )
            }
        }
        composeRule.onNodeWithText(DemoModeCatalog.SETTINGS_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOn()

        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).performClick()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOff()
        assertFalse(flags.isEnabled(FlagKeys.DEMO_MODE))

        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).performClick()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOn()
        assertTrue(flags.isEnabled(FlagKeys.DEMO_MODE))
    }
}
