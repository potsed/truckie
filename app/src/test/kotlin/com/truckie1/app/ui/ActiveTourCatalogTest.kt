package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: features/active_tour.feature — single ActiveTour SoT (ATNA-86).
 */
class ActiveTourCatalogTest {

    private val today = LocalDate.of(2026, 11, 28)

    @Test
    fun flagKeyAndDefaultsOnConstant() {
        assertEquals("ff_active_tour", FlagKeys.ACTIVE_TOUR)
        assertEquals(FlagKeys.ACTIVE_TOUR, ActiveTourCatalog.FLAG_KEY)
        assertTrue(ActiveTourCatalog.DEFAULT_ENABLED)
        assertTrue(ActiveTourCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
    }

    @Test
    fun realAndDemoShareSingleTourInstance() {
        val real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository())
        val demo = DemoAppDataSource.create(today = today)

        assertTrue(ActiveTourCatalog.sharesSingleTour(real))
        assertTrue(ActiveTourCatalog.sharesSingleTour(demo))
        assertSame(real.routes, real.trips)
        assertSame(demo.routes, demo.trips)
    }

    @Test
    fun mapHomeAddStopVisibleOnPlanTripProjection() {
        val real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository())
        real.routes.createRoute(
            name = "Run",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
            start = RouteEndpoint("Depot", -27.4, 153.0),
            end = RouteEndpoint("Depot", -27.4, 153.0),
        )
        real.routes.addStop(
            RouteStop(
                id = "s1",
                addressLabel = "Yard",
                latitude = -27.5,
                longitude = 153.1,
                kind = StopKind.PICKUP_LOAD,
            ),
        )

        assertEquals(listOf("s1"), real.trips.stops().map { it.id })
        assertEquals("Yard", real.trips.stops().single().addressLabel)
    }

    @Test
    fun planTripAddStopVisibleOnMapHomeRoute() {
        val real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository())
        real.trips.addStop(
            TripStop(
                id = "t1",
                addressLabel = "Customer",
                latitude = -27.6,
                longitude = 153.2,
            ),
        )

        assertEquals(listOf("t1"), real.routes.activeRoute()!!.stops.map { it.id })
        assertEquals(listOf("t1"), real.trips.stops().map { it.id })
    }

    @Test
    fun optimiseReplaceStopsKeepsRouteAndTripAligned() {
        val real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository())
        real.routes.createRoute(name = "Run", dateChoice = RouteDateChoice.TODAY, today = today)
        real.routes.addStop(RouteStop(id = "a", addressLabel = "A", kind = StopKind.PICKUP_LOAD, latitude = -27.4, longitude = 153.0))
        real.routes.addStop(RouteStop(id = "b", addressLabel = "B", kind = StopKind.DROP_OFF_UNLOAD, latitude = -27.5, longitude = 153.1))

        val reordered = listOf(real.trips.stops()[1], real.trips.stops()[0])
        real.trips.replaceStops(reordered)
        real.trips.lastOptimizeMode = OptimizeMode.FASTEST

        assertEquals(listOf("b", "a"), real.routes.activeRoute()!!.stops.map { it.id })
        assertEquals(listOf("b", "a"), real.trips.stops().map { it.id })
        assertEquals(OptimizeMode.FASTEST, real.trips.lastOptimizeMode)
    }

    @Test
    fun demoRouteAndTripStopIdsMatchWithCargoProjection() {
        val demo = DemoAppDataSource.create(today = today)
        val routeIds = demo.routes.activeRoute()!!.stops.map { it.id }
        val tripIds = demo.trips.stops().map { it.id }
        assertEquals(routeIds, tripIds)
        assertEquals(6, tripIds.size)
        assertTrue(demo.trips.stops().any { it.cargo.isNotEmpty() })
        assertEquals(
            demo.routes.activeRoute()!!.stops.map { it.cargo.size },
            demo.trips.stops().map { it.cargo.size },
        )
    }

    @Test
    fun demoWritesRemainReadOnlyOnSharedFacade() {
        val demo = DemoAppDataSource.create(today = today)
        val before = demo.routes.activeRoute()!!.stops.map { it.id }

        demo.routes.clear()
        demo.trips.replaceStops(emptyList())
        demo.trips.addStop(TripStop(id = "hack", addressLabel = "Hack"))
        demo.trips.lastOptimizeMode = OptimizeMode.CHEAPEST

        assertEquals(before, demo.routes.activeRoute()!!.stops.map { it.id })
        assertEquals(before, demo.trips.stops().map { it.id })
        assertEquals(null, demo.trips.lastOptimizeMode)
    }

    @Test
    fun dualModeKeepsSingleTourPerSide() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.ACTIVE_TOUR to true))
        val dual = DualModeAppDataSource(
            real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository()),
            demo = DemoAppDataSource.create(today = today),
            flags = flags,
        )
        assertTrue(ActiveTourCatalog.sharesSingleTour(dual.realSource))
        assertTrue(ActiveTourCatalog.sharesSingleTour(dual.demoSource))
        assertFalse(ActiveTourCatalog.isEnabled(LocalFeatureFlags()))
        ActiveTourCatalog.setEnabled(flags, true)
        assertTrue(ActiveTourCatalog.isEnabled(flags))
    }
}
