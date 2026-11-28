package com.truckie1.app.profiles

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Gherkin: features/room_route_persist.feature — Room ActiveTour hydrate / persist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RoomActiveTourTest {
    private lateinit var db: VehicleDatabase
    private val today = LocalDate.of(2026, 11, 28)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = VehicleDatabase.inMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun persistsRouteStopsAndSurvivesNewSession() {
        val first = RoomActiveTour(db.activeTourDao())
        first.createRoute(
            name = "Airport run",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
            start = RouteEndpoint("Depot", -27.4, 153.0),
            end = RouteEndpoint("Yard end", -27.45, 153.05),
        )
        first.addStop(
            RouteStop(
                id = "s1",
                addressLabel = "Pickup",
                latitude = -27.5,
                longitude = 153.1,
                kind = StopKind.PICKUP_LOAD,
                cargo = listOf(Cargo("Crates", 1.0, 1.0, 1.0, 0.4, CargoAction.PICK_UP)),
            ),
        )
        first.addStop(
            RouteStop(
                id = "s2",
                addressLabel = "Drop",
                latitude = -27.55,
                longitude = 153.15,
                kind = StopKind.DROP_OFF_UNLOAD,
            ),
        )
        first.lastOptimizeMode = OptimizeMode.FASTEST
        val routeId = first.activeRoute()!!.id

        val second = RoomActiveTour(db.activeTourDao())
        val restored = second.activeRoute()!!
        assertEquals(routeId, restored.id)
        assertEquals("Airport run", restored.name)
        assertEquals("Depot", restored.start!!.addressLabel)
        assertEquals("Yard end", restored.end!!.addressLabel)
        assertEquals(listOf("s1", "s2"), restored.stops.map { it.id })
        assertEquals(StopKind.PICKUP_LOAD, restored.stops[0].kind)
        assertEquals(1, restored.stops[0].cargo.size)
        assertEquals("Crates", restored.stops[0].cargo.single().description)
        assertEquals(listOf("s1", "s2"), second.stops().map { it.id })
        assertEquals(OptimizeMode.FASTEST, second.lastOptimizeMode)
    }

    @Test
    fun clearRemovesPersistedTour() {
        val first = RoomActiveTour(db.activeTourDao())
        first.createRoute(
            name = "Temp",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
            start = RouteEndpoint("A", -27.0, 153.0),
            end = RouteEndpoint("B", -27.1, 153.1),
        )
        assertTrue(first.activeRoute() != null)
        first.clear()

        val second = RoomActiveTour(db.activeTourDao())
        assertNull(second.activeRoute())
        assertTrue(second.stops().isEmpty())
        assertNull(second.lastOptimizeMode)
    }
}
