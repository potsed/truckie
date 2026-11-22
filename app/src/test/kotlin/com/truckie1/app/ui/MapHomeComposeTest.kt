package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Gherkin: map_first_home.feature — Compose sheet body (create route / stop kinds).
 * Sheet body is tested directly so Robolectric height constraints do not clip content.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapHomeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun halfSheetShowsCreateRouteWhenNoActiveRoute() {
        composeRule.setContent {
            TruckieTheme {
                RouteSheetBody(
                    route = null,
                    sheetValue = RouteSheetValue.Half,
                    onCreateRoute = { _, _, _ -> },
                    onAddStop = { _, _, _ -> },
                )
            }
        }
        composeRule.onNodeWithTag("create_route_title").assertIsDisplayed()
        composeRule.onNodeWithTag("date_today").assertIsDisplayed()
        composeRule.onNodeWithTag("date_tomorrow").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_route_button").assertIsDisplayed()
    }

    @Test
    fun halfSheetShowsStopListWithPickupAndDropOffKinds() {
        val route = Route(
            id = "r1",
            name = "Friday Route",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = "1 Queen St",
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
                RouteStop(
                    id = "s2",
                    addressLabel = "100 Eagle St",
                    kind = StopKind.DROP_OFF_UNLOAD,
                    order = 1,
                ),
            ),
        )
        composeRule.setContent {
            TruckieTheme {
                RouteSheetBody(
                    route = route,
                    sheetValue = RouteSheetValue.Half,
                    onCreateRoute = { _, _, _ -> },
                    onAddStop = { _, _, _ -> },
                )
            }
        }
        composeRule.onNodeWithTag("add_find_stops_title").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_pickup_load").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_drop_off_unload").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_list_title").assertIsDisplayed()
        composeRule.onNodeWithText("1 Queen St").assertIsDisplayed()
        composeRule.onNodeWithText("100 Eagle St").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_drop_off_unload").performClick()
    }
}
