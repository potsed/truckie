package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature — verified start/end from autocomplete.
 */
class VerifiedRouteEndpointBuilderTest {

    private val brisbane = AddressSuggestion(
        label = "1 Queen Street, Brisbane City, Queensland, 4000, Australia",
        placeId = "1",
        latitude = -27.47,
        longitude = 153.02,
    )

    @Test
    fun buildsEndpointFromVerifiedSuggestion() {
        when (val result = VerifiedRouteEndpointBuilder.build(brisbane)) {
            is VerifiedRouteEndpointBuilder.Result.Ok -> {
                assertEquals(brisbane.label, result.endpoint.addressLabel)
                assertEquals(-27.47, result.endpoint.latitude, 0.0)
                assertEquals(153.02, result.endpoint.longitude, 0.0)
            }
            is VerifiedRouteEndpointBuilder.Result.Err -> error(result.message)
        }
    }

    @Test
    fun rejectsUnverifiedSuggestion() {
        val incomplete = AddressSuggestion(label = "Somewhere", latitude = null, longitude = null)
        when (val result = VerifiedRouteEndpointBuilder.build(incomplete)) {
            is VerifiedRouteEndpointBuilder.Result.Err -> {
                assertTrue(result.message.contains("verified", ignoreCase = true))
            }
            is VerifiedRouteEndpointBuilder.Result.Ok -> error("expected Err")
        }
    }

    @Test
    fun rejectsNullSuggestion() {
        when (val result = VerifiedRouteEndpointBuilder.build(null)) {
            is VerifiedRouteEndpointBuilder.Result.Err -> {
                assertTrue(result.message.isNotBlank())
            }
            is VerifiedRouteEndpointBuilder.Result.Ok -> error("expected Err")
        }
    }
}
