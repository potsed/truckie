package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: create_route_start_end.feature — ActiveTourSession start/end create.
 */
class RouteStartEndSessionTest {

    private val depot = RouteEndpoint(
        addressLabel = "12 Depot Rd, Brisbane",
        latitude = -27.45,
        longitude = 153.01,
    )
    private val customer = RouteEndpoint(
        addressLabel = "100 Eagle St, Brisbane",
        latitude = -27.47,
        longitude = 153.03,
    )

    @Test
    fun createRoutePersistsVerifiedStartAndEnd() {
        val session = ActiveTourSession()
        val today = LocalDate.of(2026, 11, 22)

        val route = session.createRoute(
            name = "Monday run",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
            start = depot,
            end = customer,
        )

        assertEquals(depot, route.start)
        assertEquals(customer, route.end)
        assertFalse(route.isRoundTrip)
        assertEquals(route, session.activeRoute())
    }

    @Test
    fun createRouteAllowsRoundTripSameStartAndEnd() {
        val session = ActiveTourSession()
        val route = session.createRoute(
            name = "Round trip",
            dateChoice = RouteDateChoice.TOMORROW,
            today = LocalDate.of(2026, 11, 22),
            start = depot,
            end = depot,
        )

        assertTrue(route.isRoundTrip)
        assertEquals(depot.addressLabel, route.start!!.addressLabel)
        assertEquals(depot.addressLabel, route.end!!.addressLabel)
        assertTrue(RouteSummary.strip(route).contains("round trip", ignoreCase = true))
    }

    @Test
    fun summaryMentionsStartAndEndWhenPresent() {
        val session = ActiveTourSession()
        session.createRoute(
            name = "Run",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 22),
            start = depot,
            end = customer,
        )
        val strip = RouteSummary.strip(session.activeRoute()!!)
        assertTrue(strip.contains("12 Depot Rd", ignoreCase = true) || strip.contains("start", ignoreCase = true))
        assertTrue(strip.contains("100 Eagle St", ignoreCase = true) || strip.contains("end", ignoreCase = true))
    }
}
