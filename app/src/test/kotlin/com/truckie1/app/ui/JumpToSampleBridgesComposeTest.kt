package com.truckie1.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/jump_to_sample_bridges.feature — FAB removed from map chrome.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JumpToSampleBridgesComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun jumpControlNeverShownOnMapHome() {
        composeRule.setContent {
            TruckieTheme {
                MapHomeScreen(
                    state = MapHomeUiState(route = null),
                    onSheetValueChange = {},
                    onCreateRoute = {},
                    onAddStop = { _, _, _ -> },
                    showBridgeMarkers = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithTag(JumpToSampleBridges.TEST_TAG).assertDoesNotExist()
    }
}
