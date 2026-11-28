package com.truckie1.app.profiles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Device/emulator Room ActiveTour persist (ATNA-89).
 * Requires ANDROID_HOME / connected device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class RoomActiveTourInstrumentedTest {
    private lateinit var db: VehicleDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = VehicleDatabase.inMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun persistsAcrossSessionsOnDevice() {
        val today = LocalDate.of(2026, 11, 28)
        val first = RoomActiveTour(db.activeTourDao())
        first.createRoute(
            name = "Device run",
            dateChoice = RouteDateChoice.TODAY,
            today = today,
            start = RouteEndpoint("Depot", -27.4, 153.0),
            end = RouteEndpoint("Depot", -27.4, 153.0),
        )
        first.addStop(
            RouteStop(
                id = "dev-1",
                addressLabel = "Stop",
                latitude = -27.5,
                longitude = 153.1,
                kind = StopKind.DROP_OFF_UNLOAD,
            ),
        )

        val second = RoomActiveTour(db.activeTourDao())
        assertEquals("Device run", second.activeRoute()!!.name)
        assertEquals(listOf("dev-1"), second.stops().map { it.id })
    }
}
