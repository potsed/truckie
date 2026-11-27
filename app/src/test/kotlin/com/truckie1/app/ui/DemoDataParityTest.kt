package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import java.time.LocalDate

/**
 * Gherkin: features/demo_mode.feature — data-source parity, read-only, DEMO MODE pill (ATNA-75).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DemoDataParityTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun demoDatasetLabelsDoNotRevealDemoInProductCopy() {
        assertFalse(
            DemoDataset.hasDemoRevealingProductCopy(
                routeName = DemoDataset.ROUTE_NAME,
                vehicleNickname = DemoDataset.vehicle.nickname,
                vehicleNotes = DemoDataset.vehicle.notes,
            ),
        )
        assertFalse(DemoDataset.ROUTE_NAME.contains("demo", ignoreCase = true))
        assertFalse(DemoDataset.VEHICLE_REGO.contains("demo", ignoreCase = true))
        assertFalse(DemoDataset.vehicle.nickname.contains("demo", ignoreCase = true))
        assertFalse(DemoDataset.vehicle.make.contains("demo", ignoreCase = true))
        assertEquals("Airport run", DemoDataset.ROUTE_NAME)
        assertEquals("Rigid 3.5", DemoDataset.vehicle.nickname)
    }

    @Test
    fun demoStoresRejectMutations() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)
        flags.setEnabled(FlagKeys.DEMO_MODE, true)

        val beforeVehicle = data.vehicles.all()
        val beforeRoute = data.routes.activeRoute()!!
        val beforeTrips = data.trips.stops()

        data.vehicles.save(
            DemoDataset.vehicle.copy(nickname = "Hacked", heightM = 9.9),
        )
        assertFalse(data.vehicles.setActive("REAL99"))
        data.vehicles.activeRego = "REAL99"

        data.routes.replaceActive(null)
        data.routes.addStop(
            RouteStop(
                id = "hack",
                addressLabel = "Hack St",
                kind = StopKind.PICKUP_LOAD,
            ),
        )
        data.routes.clear()

        data.trips.replaceStops(emptyList())
        data.trips.addStop(TripStop(id = "hack-trip", addressLabel = "Hack"))
        data.trips.lastOptimizeMode = com.truckie1.trips.OptimizeMode.FASTEST

        assertEquals(beforeVehicle.map { it.rego }, data.vehicles.all().map { it.rego })
        assertEquals(3.5, data.vehicles.all().first().heightM, 0.001)
        assertEquals("Rigid 3.5", data.vehicles.all().first().nickname)
        assertEquals(beforeRoute.id, data.routes.activeRoute()?.id)
        assertEquals(6, data.routes.activeRoute()?.stops?.size)
        assertEquals(beforeTrips.map { it.id }, data.trips.stops().map { it.id })
        assertEquals(null, data.trips.lastOptimizeMode)
    }

    @Test
    fun realSourceStillAcceptsWritesWhenDemoOff() {
        val flags = LocalFeatureFlags()
        val data = dual(flags)
        assertFalse(DemoModeCatalog.isEnabled(flags))

        data.routes.createRoute(
            name = "Writable",
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.of(2026, 11, 27),
            start = RouteEndpoint("A", -27.0, 153.0),
            end = RouteEndpoint("A", -27.0, 153.0),
        )
        assertEquals("Writable", data.routes.activeRoute()?.name)
        data.vehicles.save(realVehicle().copy(nickname = "Updated"))
        assertEquals("Updated", data.vehicles.findByRego("REAL99")!!.nickname)
    }

    @Test
    fun demoModePillShowsOnlyWhenEnabled() {
        assertEquals("DEMO MODE", DemoModePillCatalog.LABEL)
        assertFalse(DemoModePillCatalog.shouldShow(LocalFeatureFlags()))

        val on = LocalFeatureFlags(mapOf(FlagKeys.DEMO_MODE to true))
        assertTrue(DemoModePillCatalog.shouldShow(on))

        composeRule.setContent {
            TruckieTheme {
                DemoModePill()
            }
        }
        composeRule.onNodeWithTag(DemoModePillCatalog.TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("DEMO MODE").assertIsDisplayed()
    }

    @Test
    fun controllerOnlyFlipsFlagAndRevision() {
        val flags = LocalFeatureFlags()
        val controller = DemoModeController(flags)
        assertEquals(0, controller.revision())
        controller.setEnabled(true)
        assertTrue(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertEquals(1, controller.revision())
        controller.setEnabled(true) // no-op
        assertEquals(1, controller.revision())
        controller.setEnabled(false)
        assertFalse(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertEquals(2, controller.revision())
    }

    @Test
    fun demoSourceIsPreSeededWithoutController() {
        val demo = DemoAppDataSource.create(today = LocalDate.of(2026, 11, 27))
        assertNotNull(demo.routes.activeRoute())
        assertEquals(6, demo.routes.activeRoute()!!.stops.size)
        assertEquals(DemoDataset.depot, demo.routes.activeRoute()!!.start)
        assertEquals(listOf(DemoDataset.VEHICLE_REGO), demo.vehicles.all().map { it.rego })
        assertEquals(6, demo.trips.stops().size)
        assertTrue(demo.trips.stops().any { it.cargo.isNotEmpty() })
        assertTrue(DemoDataset.supportsReadOnlyDogfood())
    }

    @Test
    fun seededDemoCoversReadOnlyFeatureWalkthrough() {
        // Daniel: mutations need not work; seed must exercise features without writes.
        assertTrue(DemoDataset.supportsReadOnlyDogfood())
        val flags = LocalFeatureFlags(mapOf(FlagKeys.DEMO_MODE to true))
        val data = dual(flags)
        val route = data.routes.activeRoute()!!
        assertTrue(route.hasEndpoints)
        assertEquals(6, data.trips.stops().size)
        assertTrue(data.trips.stops().all { it.latitude != null && it.longitude != null })
        assertEquals(3.5, data.vehicles.all().single().heightM, 0.001)
        // Optimize / Navigate / clearance can read this seed; writes remain no-ops.
        data.trips.replaceStops(emptyList())
        assertEquals(6, data.trips.stops().size)
    }
}
