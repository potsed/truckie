package com.truckie1.app.ui

import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteSession
import com.truckie1.trips.TripSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: features/demo_mode.feature — hard isolation of demo vs real data (ATNA-56).
 */
class DemoModeIsolationTest {

    private fun realVehicle() = Vehicle(
        rego = "REAL99",
        nickname = "Real truck",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = 2020,
        heightM = 4.0,
        widthM = 2.5,
        lengthM = 10.0,
        massT = 10.0,
        tareT = 6.0,
        gvmT = 15.0,
        gcmT = 15.0,
    )

    @Test
    fun enablingDemoHidesRealVehicleAndRoute() {
        val flags = LocalFeatureFlags()
        val realRepo = InMemoryVehicleProfileRepository()
        realRepo.save(realVehicle())
        val dual = DualModeVehicleProfileRepository(real = realRepo)
        val routes = RouteSession()
        routes.createRoute(
            name = "Real job",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 25),
            start = RouteEndpoint("Real depot", -27.5, 153.0),
            end = RouteEndpoint("Real depot", -27.5, 153.0),
        )
        val trips = TripSession()
        val controller = DemoModeController(
            flags = flags,
            vehicles = dual,
            routeSession = routes,
            tripSession = trips,
        )

        assertEquals(listOf("REAL99"), dual.all().map { it.rego })
        assertEquals("Real job", routes.activeRoute()?.name)

        controller.setEnabled(true)

        assertTrue(DemoModeCatalog.isEnabled(flags))
        assertEquals(listOf(DemoDataset.VEHICLE_REGO), dual.all().map { it.rego })
        assertEquals(3.5, dual.findByRego(DemoDataset.VEHICLE_REGO)!!.heightM, 0.001)
        assertNull(dual.findByRego("REAL99"))
        val demoRoute = routes.activeRoute()
        assertNotNull(demoRoute)
        assertTrue(DemoDataset.isDemoRouteId(demoRoute!!.id))
        assertEquals(DemoDataset.depot, demoRoute.start)
        assertEquals(6, demoRoute.stops.size)
        assertFalse(demoRoute.name == "Real job")
    }

    @Test
    fun disablingDemoRestoresRealDataAndHidesDemo() {
        val flags = LocalFeatureFlags()
        val realRepo = InMemoryVehicleProfileRepository()
        realRepo.save(realVehicle())
        val dual = DualModeVehicleProfileRepository(real = realRepo)
        val routes = RouteSession()
        val real = routes.createRoute(
            name = "Real job",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 25),
        )
        val trips = TripSession()
        val controller = DemoModeController(
            flags = flags,
            vehicles = dual,
            routeSession = routes,
            tripSession = trips,
        )

        controller.setEnabled(true)
        assertTrue(DemoDataset.isDemoRouteId(routes.activeRoute()!!.id))

        controller.setEnabled(false)

        assertFalse(DemoModeCatalog.isEnabled(flags))
        assertEquals(listOf("REAL99"), dual.all().map { it.rego })
        assertNull(dual.findByRego(DemoDataset.VEHICLE_REGO))
        assertEquals(real.id, routes.activeRoute()?.id)
        assertEquals("Real job", routes.activeRoute()?.name)
        assertFalse(DemoDataset.isDemoRouteId(routes.activeRoute()!!.id))
    }

    @Test
    fun dualRepoNeverLeaksDemoIntoRealStore() {
        val realRepo = InMemoryVehicleProfileRepository()
        realRepo.save(realVehicle())
        val dual = DualModeVehicleProfileRepository(real = realRepo)
        dual.demoModeEnabled = true
        dual.save(DemoDataset.vehicle)
        assertEquals(1, dual.all().size)
        dual.demoModeEnabled = false
        assertEquals(listOf("REAL99"), realRepo.all().map { it.rego })
        assertFalse(realRepo.all().any { DemoDataset.isDemoVehicleRego(it.rego) })
    }
}
