package com.truckie1.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.truckie1.trips.Route
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/pull_up_drawer_scroll.feature —
 * last stop reachable inside a phone-sized map-home viewport.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PullUpDrawerScrollComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fullSheetLastStopIsReachableWithinPhoneViewport() {
        val stops = (0 until 8).map { index ->
            RouteStop(
                id = "s$index",
                addressLabel = "Stop address ${index + 1}",
                kind = if (index % 2 == 0) StopKind.PICKUP_LOAD else StopKind.DROP_OFF_UNLOAD,
                order = index,
            )
        }
        val route = Route(
            id = "r-scroll",
            name = "Demo run",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            stops = stops,
        )
        composeRule.setContent {
            TruckieTheme {
                // Phone-ish host — must not rely on a 1600dp tall escape hatch.
                Box(modifier = Modifier.size(390.dp, 780.dp)) {
                    MapHomeScreen(
                        state = MapHomeUiState(
                            route = route,
                            sheetValue = RouteSheetValue.Full,
                            summaryStrip = "8 stops",
                        ),
                        onSheetValueChange = {},
                        onCreateRoute = {},
                        onAddStop = { _, _, _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("route_drag_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_list_title").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_row_s7").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun jumpFabNeverAppearsOnMapHomeEvenWhenRequested() {
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.size(390.dp, 780.dp)) {
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
        }
        composeRule.onNodeWithTag(JumpToSampleBridges.TEST_TAG).assertDoesNotExist()
    }
}
