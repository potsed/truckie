package com.truckie1.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.truckie1.app.trips.AddressAutocomplete
import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: create_route_start_end.feature — start/end + Navigate sheet UX.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CreateRouteStartEndComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val depot = AddressSuggestion(
        label = "12 Depot Rd, Brisbane",
        placeId = "d1",
        latitude = -27.45,
        longitude = 153.01,
    )
    private val customer = AddressSuggestion(
        label = "100 Eagle St, Brisbane",
        placeId = "c1",
        latitude = -27.47,
        longitude = 153.03,
    )
    private val autocomplete = AddressAutocomplete { query ->
        if (query.isBlank()) emptyList()
        else listOf(depot, customer).filter { it.label.contains(query, ignoreCase = true) }
    }

    @Test
    fun createRouteRequiresVerifiedStartAndEndWhenFlagged() {
        var created: CreateRouteInput? = null
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.size(420.dp, 2200.dp)) {
                    RouteSheetBody(
                        route = null,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { created = it },
                        onAddStop = { _, _, _ -> },
                        autocomplete = autocomplete,
                        requireStartEnd = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("route_start_field").assertIsDisplayed()
        composeRule.onNodeWithTag("route_round_trip_checkbox").assertIsDisplayed()
        composeRule.onNodeWithTag("route_end_field").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_route_button").performScrollTo().assertIsNotEnabled()

        composeRule.onNodeWithTag("route_start_field").performTextInput("Depot")
        composeRule.onNodeWithTag("route_start_suggestion").performClick()
        composeRule.onNodeWithTag("route_end_field").performScrollTo().performTextInput("Eagle")
        composeRule.onNodeWithTag("route_end_suggestion").performClick()
        composeRule.onNodeWithTag("confirm_route_button").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("confirm_route_button").performClick()

        assertNotNull(created)
        assertEquals(depot, created!!.start)
        assertEquals(customer, created!!.end)
        assertTrue(!created!!.roundTrip)
    }

    @Test
    fun roundTripCopiesStartToEnd() {
        var created: CreateRouteInput? = null
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.size(420.dp, 2200.dp)) {
                    RouteSheetBody(
                        route = null,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { created = it },
                        onAddStop = { _, _, _ -> },
                        autocomplete = autocomplete,
                        requireStartEnd = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("route_start_field").performTextInput("Depot")
        composeRule.onNodeWithTag("route_start_suggestion").performClick()
        composeRule.onNodeWithTag("route_round_trip_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirm_route_button").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("confirm_route_button").performClick()
        assertEquals(depot, created!!.start)
        assertEquals(depot, created!!.end)
        assertTrue(created!!.roundTrip)
    }

    @Test
    fun navigateButtonShowsWhenFlaggedAndEnabledWithEndpoints() {
        val route = Route(
            id = "r1",
            name = "Run",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = RouteEndpoint(depot.label, depot.latitude!!, depot.longitude!!),
            end = RouteEndpoint(customer.label, customer.latitude!!, customer.longitude!!),
        )
        var navigated = false
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.size(420.dp, 1600.dp)) {
                    RouteSheetBody(
                        route = route,
                        sheetValue = RouteSheetValue.Half,
                        onCreateRoute = { _ -> },
                        onAddStop = { _, _, _ -> },
                        onNavigateRoute = { navigated = true },
                        showNavigateAction = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
        composeRule.onNodeWithTag("navigate_route_button").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("navigate_route_button").performClick()
        assertTrue(navigated)
    }
}
