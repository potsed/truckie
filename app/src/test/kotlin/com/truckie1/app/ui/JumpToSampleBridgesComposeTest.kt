package com.truckie1.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/jump_to_sample_bridges.feature — map chrome control visibility.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JumpToSampleBridgesComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun jumpControlHiddenWhenDemoModeOff() {
        composeRule.setContent {
            TruckieTheme {
                MapHomeScreen(
                    state = MapHomeUiState(route = null),
                    onSheetValueChange = {},
                    onCreateRoute = {},
                    onAddStop = { _, _, _ -> },
                    showBridgeMarkers = true,
                    showJumpToSampleBridges = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithTag(JumpToSampleBridges.TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun jumpControlVisibleAndClickableWhenDemoModeOn() {
        composeRule.setContent {
            TruckieTheme {
                MapHomeScreen(
                    state = MapHomeUiState(route = null),
                    onSheetValueChange = {},
                    onCreateRoute = {},
                    onAddStop = { _, _, _ -> },
                    showBridgeMarkers = true,
                    showJumpToSampleBridges = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithTag(JumpToSampleBridges.TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(JumpToSampleBridges.SHORT_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(JumpToSampleBridges.TEST_TAG).performClick()
    }
}
