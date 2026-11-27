package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: features/demo_mode.feature — hard isolation via data source swap (ATNA-75).
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

    private fun dual(flags: LocalFeatureFlags): DualModeAppDataSource {
        val realRepo = InMemoryVehicleProfileRepository()
        realRepo.save(realVehicle())
        return DualModeAppDataSource(
            real = RealAppDataSource(vehicles = realRepo),
            demo = DemoAppDataSource.create(today = LocalDate.of(2026, 11, 27)),
            flags = flags,
        )
    }

    @Test
    fun enablingDemoHidesRealVehicleAndRoute() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)
        data.routes.createRoute(
            name = "Real job",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 25),
            start = RouteEndpoint("Real depot", -27.5, 153.0),
            end = RouteEndpoint("Real depot", -27.5, 153.0),
        )
        val controller = DemoModeController(flags = flags)

        assertEquals(listOf("REAL99"), data.vehicles.all().map { it.rego })
        assertEquals("Real job", data.routes.activeRoute()?.name)

        controller.setEnabled(true)

        assertTrue(DemoModeCatalog.isEnabled(flags))
        assertEquals(listOf(DemoDataset.VEHICLE_REGO), data.vehicles.all().map { it.rego })
        assertEquals(3.5, data.vehicles.findByRego(DemoDataset.VEHICLE_REGO)!!.heightM, 0.001)
        assertNull(data.vehicles.findByRego("REAL99"))
        val demoRoute = data.routes.activeRoute()
        assertNotNull(demoRoute)
        assertTrue(DemoDataset.isDemoRouteId(demoRoute!!.id))
        assertEquals(DemoDataset.depot, demoRoute.start)
        assertEquals(6, demoRoute.stops.size)
        assertFalse(demoRoute.name == "Real job")
        assertFalse(
            DemoDataset.hasDemoRevealingProductCopy(
                routeName = demoRoute.name,
                vehicleNickname = data.vehicles.all().first().nickname,
                vehicleNotes = data.vehicles.all().first().notes,
            ),
        )
    }

    @Test
    fun disablingDemoRestoresRealDataAndHidesDemo() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)
        val real = data.routes.createRoute(
            name = "Real job",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 25),
        )
        val controller = DemoModeController(flags = flags)

        controller.setEnabled(true)
        assertTrue(DemoDataset.isDemoRouteId(data.routes.activeRoute()!!.id))

        controller.setEnabled(false)

        assertFalse(DemoModeCatalog.isEnabled(flags))
        assertEquals(listOf("REAL99"), data.vehicles.all().map { it.rego })
        assertNull(data.vehicles.findByRego(DemoDataset.VEHICLE_REGO))
        assertEquals(real.id, data.routes.activeRoute()?.id)
        assertEquals("Real job", data.routes.activeRoute()?.name)
        assertFalse(DemoDataset.isDemoRouteId(data.routes.activeRoute()!!.id))
    }

    @Test
    fun dualSourceFollowsFlagWithoutSeedRepairPath() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)

        assertEquals(listOf("REAL99"), data.vehicles.all().map { it.rego })

        // Flag alone swaps stores — no ensureSeededIfDemo / backup path.
        flags.setEnabled(FlagKeys.DEMO_MODE, true)
        assertEquals(listOf(DemoDataset.VEHICLE_REGO), data.vehicles.all().map { it.rego })
        assertEquals(3.5, data.vehicles.findByRego(DemoDataset.VEHICLE_REGO)!!.heightM, 0.001)
        assertNull(data.vehicles.findByRego("REAL99"))
        assertTrue(DemoDataset.isDemoRouteId(data.routes.activeRoute()!!.id))
        assertEquals(6, data.routes.activeRoute()!!.stops.size)

        flags.setEnabled(FlagKeys.DEMO_MODE, false)
        assertEquals(listOf("REAL99"), data.vehicles.all().map { it.rego })
        assertFalse(data.vehicles.all().any { DemoDataset.isDemoVehicleRego(it.rego) })
    }

    @Test
    fun realAndDemoShareIdenticalDomainTypes() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)
        flags.setEnabled(FlagKeys.DEMO_MODE, false)
        val realVehicle: Vehicle = data.vehicles.all().first()
        val realRoute: Route? = data.routes.activeRoute()

        flags.setEnabled(FlagKeys.DEMO_MODE, true)
        val demoVehicle: Vehicle = data.vehicles.all().first()
        val demoRoute: Route = data.routes.activeRoute()!!
        val demoStops: List<TripStop> = data.trips.stops()

        assertSame(Vehicle::class.java, realVehicle::class.java)
        assertSame(Vehicle::class.java, demoVehicle::class.java)
        assertSame(Route::class.java, demoRoute::class.java)
        assertTrue(demoStops.isNotEmpty())
        assertSame(TripStop::class.java, demoStops.first()::class.java)
        // realRoute may be null before create — type contract still Vehicle/Route/TripStop.
        assertTrue(realRoute == null || realRoute::class.java == Route::class.java)
    }
}
