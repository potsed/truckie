package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature — verified start/end + round trip.
 */
class RouteEndpointTest {

    @Test
    fun verifiedEndpointStoresLabelAndCoordinates() {
        val endpoint = RouteEndpoint(
            addressLabel = "1 Queen St, Brisbane",
            latitude = -27.47,
            longitude = 153.02,
        )
        assertEquals("1 Queen St, Brisbane", endpoint.addressLabel)
        assertEquals(-27.47, endpoint.latitude, 0.0)
        assertEquals(153.02, endpoint.longitude, 0.0)
    }

    @Test
    fun samePlaceComparesCoordinates() {
        val depot = RouteEndpoint("Depot", -27.47, 153.02)
        val same = RouteEndpoint("Depot yard", -27.47, 153.02)
        val other = RouteEndpoint("Other", -27.48, 153.02)

        assertTrue(depot.samePlaceAs(same))
        assertFalse(depot.samePlaceAs(other))
    }

    @Test
    fun routeIsRoundTripWhenStartAndEndAreSamePlace() {
        val place = RouteEndpoint("Home depot", -27.5, 153.0)
        val roundTrip = Route(
            id = "r1",
            dateEpochDay = 20_000,
            start = place,
            end = place.copy(),
        )
        val oneWay = Route(
            id = "r2",
            dateEpochDay = 20_000,
            start = place,
            end = RouteEndpoint("Customer", -27.6, 153.1),
        )

        assertTrue(roundTrip.hasEndpoints)
        assertTrue(roundTrip.isRoundTrip)
        assertTrue(oneWay.hasEndpoints)
        assertFalse(oneWay.isRoundTrip)
    }

    @Test
    fun routeWithoutEndpointsIsNotReady() {
        val draft = Route(id = "r0", dateEpochDay = 20_000)
        assertFalse(draft.hasEndpoints)
        assertFalse(draft.isRoundTrip)
    }
}
