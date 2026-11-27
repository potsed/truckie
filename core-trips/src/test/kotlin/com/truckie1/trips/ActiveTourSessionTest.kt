package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: active_tour.feature — single ActiveTour SoT for Route + TripStop projection.
 */
class ActiveTourSessionTest {

    private val today = LocalDate.of(2026, 11, 28)

    @Test
    fun createRouteAndAddStopVisibleViaTripProjection() {
        val tour = ActiveTourSession()
        assertNull(tour.activeRoute())
        assertTrue(tour.stops().isEmpty())

        tour.createRoute(name = "Airport run", dateChoice = RouteDateChoice.TODAY, today = today)
        tour.addStop(
            RouteStop(
                id = "s1",
                addressLabel = "1 Queen St",
                latitude = -27.47,
                longitude = 153.02,
                kind = StopKind.PICKUP_LOAD,
                cargo = listOf(Cargo("Crates", 3.5, 2.0, 3.0, 1.0, CargoAction.PICK_UP)),
            ),
        )

        assertEquals(1, tour.activeRoute()!!.stops.size)
        assertEquals(listOf("s1"), tour.stops().map { it.id })
        assertEquals(1, tour.stops().first().cargo.size)
        assertEquals(-27.47, tour.stops().first().latitude!!, 0.0001)
    }

    @Test
    fun addTripStopAppearsOnActiveRoute() {
        val tour = ActiveTourSession()
        tour.createRoute(name = null, dateChoice = RouteDateChoice.TODAY, today = today)

        tour.addStop(
            TripStop(
                id = "t1",
                addressLabel = "Yard",
                latitude = -27.5,
                longitude = 153.1,
                cargo = listOf(Cargo("Beams", 2.0, 1.0, 4.0, 2.0, CargoAction.DROP_OFF)),
            ),
        )

        val routeStops = tour.activeRoute()!!.stops
        assertEquals(1, routeStops.size)
        assertEquals("t1", routeStops.first().id)
        assertEquals(StopKind.DROP_OFF_UNLOAD, routeStops.first().kind)
        assertEquals(1, routeStops.first().cargo.size)
    }

    @Test
    fun addTripStopWithoutRouteCreatesDraftThenActivates() {
        val tour = ActiveTourSession()
        tour.addStop(TripStop(id = "orphan", addressLabel = "Depot", latitude = -27.4, longitude = 153.0))

        val route = tour.activeRoute()
        // Shell is auto-created as DRAFT; first stop promotes to ACTIVE (RouteSession parity).
        assertEquals(RouteStatus.ACTIVE, route!!.status)
        assertEquals(1, route.stops.size)
        assertEquals("orphan", route.stops.first().id)
    }

    @Test
    fun replaceStopsReordersSharedRoute() {
        val tour = ActiveTourSession()
        tour.createRoute(name = "Run", dateChoice = RouteDateChoice.TODAY, today = today)
        tour.addStop(RouteStop(id = "a", addressLabel = "A", kind = StopKind.PICKUP_LOAD, latitude = -27.4, longitude = 153.0))
        tour.addStop(RouteStop(id = "b", addressLabel = "B", kind = StopKind.DROP_OFF_UNLOAD, latitude = -27.5, longitude = 153.1))
        tour.addStop(RouteStop(id = "c", addressLabel = "C", kind = StopKind.DROP_OFF_UNLOAD, latitude = -27.6, longitude = 153.2))

        val reordered = listOf(
            tour.stops()[0],
            tour.stops()[2],
            tour.stops()[1],
        )
        tour.replaceStops(reordered)
        tour.lastOptimizeMode = OptimizeMode.FASTEST

        assertEquals(listOf("a", "c", "b"), tour.activeRoute()!!.stops.map { it.id })
        assertEquals(listOf("a", "c", "b"), tour.stops().map { it.id })
        assertEquals(OptimizeMode.FASTEST, tour.lastOptimizeMode)
        assertEquals(StopKind.PICKUP_LOAD, tour.activeRoute()!!.stops[0].kind)
        assertEquals(StopKind.DROP_OFF_UNLOAD, tour.activeRoute()!!.stops[1].kind)
    }

    @Test
    fun moveRemoveAndUpdateStopKeepRouteAndTripInSync() {
        val tour = ActiveTourSession()
        tour.createRoute(name = "Run", dateChoice = RouteDateChoice.TODAY, today = today)
        tour.addStop(RouteStop(id = "1", addressLabel = "Depot", kind = StopKind.PICKUP_LOAD))
        tour.addStop(RouteStop(id = "2", addressLabel = "Site A", kind = StopKind.DROP_OFF_UNLOAD))
        tour.addStop(RouteStop(id = "3", addressLabel = "Yard", kind = StopKind.DROP_OFF_UNLOAD))

        tour.moveStop(fromIndex = 2, toIndex = 1)
        assertEquals(listOf("1", "3", "2"), tour.stops().map { it.id })
        assertEquals(listOf("1", "3", "2"), tour.activeRoute()!!.stops.map { it.id })

        tour.removeStop("3")
        assertEquals(listOf("1", "2"), tour.stops().map { it.id })
        assertEquals(listOf(0, 1), tour.activeRoute()!!.stops.map { it.order })

        tour.updateStop(
            TripStop(id = "1", addressLabel = "Depot North", latitude = -27.1, longitude = 153.0),
        )
        assertEquals("Depot North", tour.activeRoute()!!.stops.first().addressLabel)
        assertEquals(-27.1, tour.stops().first().latitude!!, 0.0001)
    }

    @Test
    fun routeRepositoryAndTripRepositoryAreSameFacadeInstance() {
        val tour = ActiveTourSession()
        val asRoute: RouteRepository = tour
        val asTrip: TripRepository = tour
        assertSame(asRoute, asTrip)
    }

    @Test
    fun clearWipesRouteAndTripProjection() {
        val tour = ActiveTourSession()
        tour.createRoute(name = "X", dateChoice = RouteDateChoice.TODAY, today = today)
        tour.addStop(RouteStop(id = "1", addressLabel = "A", kind = StopKind.PICKUP_LOAD))
        tour.clear()
        assertNull(tour.activeRoute())
        assertTrue(tour.stops().isEmpty())
    }
}
