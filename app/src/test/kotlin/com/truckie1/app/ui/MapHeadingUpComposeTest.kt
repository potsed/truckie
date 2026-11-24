package com.truckie1.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.truckie1.trips.RouteSummary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/map_heading_up.feature — map orientation lock control.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapHeadingUpComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapOrientationLockControlTogglesHeadingUp() {
        var locked = true
        composeRule.setContent {
            TruckieTheme {
                var headingUp by remember { mutableStateOf(true) }
                MapHomeScreen(
                    state = MapHomeUiState(
                        route = null,
                        sheetValue = RouteSheetValue.Peek,
                        summaryStrip = RouteSummary.emptyStrip(),
                    ),
                    onSheetValueChange = {},
                    onCreateRoute = {},
                    onAddStop = { _, _, _ -> },
                    showLocationPuck = true,
                    locationFollowEnabled = true,
                    headingUpEnabled = headingUp,
                    showOrientationLock = true,
                    onOrientationLockChange = {
                        headingUp = it
                        locked = it
                    },
                )
            }
        }
        composeRule.onNodeWithTag(MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG).performClick()
        assertFalse(locked)
        composeRule.onNodeWithTag(MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG).performClick()
        assertTrue(locked)
    }
}
