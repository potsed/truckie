package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.truckie1.app.trips.AddressAutocomplete
import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.trips.Route
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: verified_address_autocomplete.feature — map-home add/find stops UI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VerifiedAddressComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val brisbane = AddressSuggestion(
        label = "1 Queen Street, Brisbane City, Queensland, 4000, Australia",
        placeId = "1",
        latitude = -27.47,
        longitude = 153.02,
    )

    private val fakeAutocomplete = AddressAutocomplete { query ->
        if (query.contains("Queen", ignoreCase = true)) listOf(brisbane) else emptyList()
    }

    private val activeRoute = Route(
        id = "r1",
        name = "Sunday",
        dateEpochDay = 20_000,
        status = RouteStatus.ACTIVE,
        stops = emptyList(),
    )

    @Test
    fun verifiedModeShowsSuggestionsAndBlocksFreeTextAdd() {
        var added: Triple<String, StopKind, AddressSuggestion?>? = null
        composeRule.setContent {
            TruckieTheme {
                RouteSheetBody(
                    route = activeRoute,
                    sheetValue = RouteSheetValue.Half,
                    onCreateRoute = { _, _, _ -> },
                    onAddStop = { label, kind, suggestion ->
                        added = Triple(label, kind, suggestion)
                    },
                    autocomplete = fakeAutocomplete,
                    requireVerifiedAddress = true,
                )
            }
        }

        composeRule.onNodeWithTag("add_find_stops_field").performTextInput("Queen")
        composeRule.onNodeWithText(brisbane.label).assertIsDisplayed()
        composeRule.onNodeWithTag("add_stop_button").assertIsNotEnabled()

        composeRule.onNodeWithText(brisbane.label).performClick()
        assertNotNull(added)
        assertEquals(brisbane.label, added!!.first)
        assertEquals(StopKind.PICKUP_LOAD, added!!.second)
        assertEquals(brisbane, added!!.third)
        assertNotNull(added!!.third!!.latitude)
        assertNotNull(added!!.third!!.longitude)
    }

    @Test
    fun freeTextModeStillAllowsAddWithoutSuggestion() {
        var added: Triple<String, StopKind, AddressSuggestion?>? = null
        composeRule.setContent {
            TruckieTheme {
                RouteSheetBody(
                    route = activeRoute,
                    sheetValue = RouteSheetValue.Half,
                    onCreateRoute = { _, _, _ -> },
                    onAddStop = { label, kind, suggestion ->
                        added = Triple(label, kind, suggestion)
                    },
                    autocomplete = AddressAutocomplete { emptyList() },
                    requireVerifiedAddress = false,
                )
            }
        }

        composeRule.onNodeWithTag("add_find_stops_field").performTextInput("Depot yard")
        composeRule.onNodeWithTag("add_stop_button").assertIsEnabled()
        composeRule.onNodeWithTag("add_stop_button").performClick()
        assertEquals("Depot yard", added!!.first)
        assertNull(added!!.third)
    }

    @Test
    fun existingStopsWithCoordsStillRenderInList() {
        val withCoords = activeRoute.copy(
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = brisbane.label,
                    latitude = brisbane.latitude,
                    longitude = brisbane.longitude,
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
            ),
        )
        composeRule.setContent {
            TruckieTheme {
                RouteSheetBody(
                    route = withCoords,
                    sheetValue = RouteSheetValue.Half,
                    onCreateRoute = { _, _, _ -> },
                    onAddStop = { _, _, _ -> },
                    requireVerifiedAddress = true,
                )
            }
        }
        composeRule.onNodeWithText(brisbane.label).assertIsDisplayed()
    }
}
