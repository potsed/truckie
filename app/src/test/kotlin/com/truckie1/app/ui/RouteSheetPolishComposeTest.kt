package com.truckie1.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
 * Gherkin: route_sheet_polish.feature — Compose hierarchy for polished sheet body.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteSheetPolishComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createRouteShowsHeadlineAndOutlinedDateCards() {
        composeRule.setContent {
            TruckieTheme {
                // Tall host so outlined date cards + Confirm are not clipped in Robolectric.
                Box(modifier = Modifier.size(420.dp, 1600.dp)) {
                    RouteSheetBody(
                        route = null,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { _, _, _ -> },
                        onAddStop = { _, _, _ -> },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("create_route_title").assertIsDisplayed()
        composeRule.onNodeWithTag("date_today_card").assertIsDisplayed()
        composeRule.onNodeWithTag("date_tomorrow_card").assertIsDisplayed()
        composeRule.onNodeWithTag("date_custom_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_route_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun activeRouteShowsSearchIconFilterChipsAndTimelineStops() {
        val route = Route(
            id = "r1",
            name = "Sunday",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = "1 Queen St, Brisbane",
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
                RouteStop(
                    id = "s2",
                    addressLabel = "100 Eagle St, Brisbane",
                    kind = StopKind.DROP_OFF_UNLOAD,
                    order = 1,
                ),
            ),
        )
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.size(420.dp, 1600.dp)) {
                    RouteSheetBody(
                        route = route,
                        sheetValue = RouteSheetValue.Full,
                        onCreateRoute = { _, _, _ -> },
                        onAddStop = { _, _, _ -> },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("add_find_stops_title").assertIsDisplayed()
        composeRule.onNodeWithTag("add_find_search_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_kind_chip_row").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_pickup_load").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_drop_off_unload").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_list_title").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("stop_timeline_badge_s1").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_timeline_badge_s2").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_kind_label_s1").assertIsDisplayed()
        composeRule.onNodeWithText("1 Queen St, Brisbane").assertIsDisplayed()
        composeRule.onNodeWithText("100 Eagle St, Brisbane").assertIsDisplayed()
    }
}
