package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: map_first_home.feature — Route → Stops domain for the map drag sheet.
 */
class RouteSessionTest {

    @Test
    fun createRouteStoresOptionalNameAndDateChoice() {
        val session = RouteSession()
        assertNull(session.activeRoute())

        val today = LocalDate.of(2026, 11, 22)
        val route = session.createRoute(
            name = "Sunday",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
        )

        assertEquals("Sunday", route.name)
        assertEquals(today.toEpochDay(), route.dateEpochDay)
        assertEquals(RouteStatus.DRAFT, route.status)
        assertEquals(route, session.activeRoute())
    }

    @Test
    fun tomorrowAndCustomDateResolveCorrectly() {
        val session = RouteSession()
        val today = LocalDate.of(2026, 11, 22)

        val tomorrow = session.createRoute(
            name = null,
            dateChoice = RouteDateChoice.TOMORROW,
            today = today,
        )
        assertNull(tomorrow.name)
        assertEquals(today.plusDays(1).toEpochDay(), tomorrow.dateEpochDay)

        val custom = session.createRoute(
            name = "Friday Route",
            dateChoice = RouteDateChoice.CUSTOM,
            today = today,
            customDate = LocalDate.of(2026, 11, 28),
        )
        assertEquals(LocalDate.of(2026, 11, 28).toEpochDay(), custom.dateEpochDay)
    }

    @Test
    fun addStopsWithPickupLoadAndDropOffUnloadKinds() {
        val session = RouteSession()
        session.createRoute(name = "Run", dateChoice = RouteDateChoice.TODAY, today = LocalDate.of(2026, 11, 22))

        session.addStop(
            RouteStop(
                id = "s1",
                addressLabel = "1 Queen St, Brisbane",
                latitude = -27.47,
                longitude = 153.02,
                kind = StopKind.PICKUP_LOAD,
            ),
        )
        session.addStop(
            RouteStop(
                id = "s2",
                addressLabel = "100 Eagle St, Brisbane",
                kind = StopKind.DROP_OFF_UNLOAD,
            ),
        )

        val stops = session.activeRoute()!!.stops
        assertEquals(2, stops.size)
        assertEquals(StopKind.PICKUP_LOAD, stops[0].kind)
        assertEquals(0, stops[0].order)
        assertEquals(StopKind.DROP_OFF_UNLOAD, stops[1].kind)
        assertEquals(1, stops[1].order)
    }

    @Test
    fun replaceActiveSwapsOrClearsActiveRoute() {
        val session = RouteSession()
        val created = session.createRoute(
            name = "Real",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 25),
        )
        assertEquals(created, session.activeRoute())

        val replacement = created.copy(id = "demo-route", name = "Demo")
        session.replaceActive(replacement)
        assertEquals("demo-route", session.activeRoute()!!.id)
        assertEquals("Demo", session.activeRoute()!!.name)

        session.replaceActive(null)
        assertNull(session.activeRoute())
    }

    @Test
    fun summaryCountsStopsByKind() {
        val session = RouteSession()
        session.createRoute(name = null, dateChoice = RouteDateChoice.TODAY, today = LocalDate.of(2026, 11, 22))
        session.addStop(
            RouteStop(id = "a", addressLabel = "A", kind = StopKind.PICKUP_LOAD),
        )
        session.addStop(
            RouteStop(id = "b", addressLabel = "B", kind = StopKind.DROP_OFF_UNLOAD),
        )
        session.addStop(
            RouteStop(id = "c", addressLabel = "C", kind = StopKind.PICKUP_LOAD),
        )

        val summary = RouteSummary.strip(session.activeRoute()!!)
        assertTrue(summary.contains("3 stops"))
        assertTrue(summary.contains("2 pickup/load"))
        assertTrue(summary.contains("1 drop-off/unload"))
    }
}
