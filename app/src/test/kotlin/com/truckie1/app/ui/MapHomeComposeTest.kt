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
import androidx.compose.ui.test.performClick
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
 * Gherkin: map_first_home.feature — Compose sheet body (create route / stop kinds).
 * Sheet body is tested in a tall scroll host so Robolectric does not clip polished layout.
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
                Box(modifier = Modifier.size(420.dp, 1600.dp)) {
                    RouteSheetBody(
                        route = null,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { _ -> },
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
        composeRule.onNodeWithTag("confirm_route_button").performScrollTo().assertIsDisplayed()
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
                Box(modifier = Modifier.size(420.dp, 1600.dp)) {
                    RouteSheetBody(
                        route = route,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { _ -> },
                        onAddStop = { _, _, _ -> },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("add_find_stops_title").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_pickup_load").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_drop_off_unload").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_list_title").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 Queen St").assertIsDisplayed()
        composeRule.onNodeWithText("100 Eagle St").assertIsDisplayed()
        composeRule.onNodeWithTag("kind_drop_off_unload").performClick()
    }
}
