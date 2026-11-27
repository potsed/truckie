package com.truckie1.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/next_destination_display.feature — Compose banner on map-home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NextDestinationDisplayComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val address = "42 South Street, Lytton QLD 4178"

    @Test
    fun bannerShowsAddressWhenProvided() {
        composeRule.setContent {
            TruckieTheme {
                NextDestinationBanner(address = address)
            }
        }
        composeRule.onNodeWithTag(NextDestinationDisplayCatalog.TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(address).assertIsDisplayed()
        // Compact phone chip: "Next · " prefix + address (single line).
        composeRule.onNodeWithText(
            "${NextDestinationDisplayCatalog.LABEL} · ",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun mapHomeShowsBannerOnlyWhenNextDestinationAddressSet() {
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = RouteEndpoint("Depot", -27.45, 153.01),
            end = RouteEndpoint("End", -27.50, 153.05),
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = address,
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
            ),
        )
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapHomeScreen(
                        state = MapHomeUiState(
                            route = route,
                            navigatePathPoints = listOf(
                                LatLngPoint(-27.45, 153.01),
                                LatLngPoint(-27.48, 153.03),
                            ),
                        ),
                        onSheetValueChange = {},
                        onCreateRoute = {},
                        onAddStop = { _, _, _ -> },
                        nextDestinationAddress = address,
                    )
                }
            }
        }
        composeRule.onNodeWithTag(NextDestinationDisplayCatalog.TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(address).assertIsDisplayed()
    }

    @Test
    fun mapHomeHidesBannerWhenAddressNull() {
        composeRule.setContent {
            TruckieTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MapHomeScreen(
                        state = MapHomeUiState(route = null),
                        onSheetValueChange = {},
                        onCreateRoute = {},
                        onAddStop = { _, _, _ -> },
                        nextDestinationAddress = null,
                    )
                }
            }
        }
        composeRule.onNodeWithTag(NextDestinationDisplayCatalog.TEST_TAG).assertDoesNotExist()
    }
}
