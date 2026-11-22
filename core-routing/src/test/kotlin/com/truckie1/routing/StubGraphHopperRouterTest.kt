package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Trip
import com.truckie1.trips.TripStop
import org.junit.Assert.assertTrue
import org.junit.Test

class StubGraphHopperRouterTest {
    private val vehicle = Vehicle(
        rego = "ABC123",
        displayName = "Rigid",
        heightM = 3.0,
        widthM = 2.5,
        lengthM = 12.5,
        grossMassT = 22.5,
    )
    private val trip = Trip(
        id = "t1",
        stops = listOf(TripStop("1", "Depot"), TripStop("2", "Yard")),
    )

    @Test
    fun rejectsLowBridge() {
        val router = StubGraphHopperRouter {
            listOf(RouteCandidate("low", 1000, 100, maxHeightM = 3.4, maxWeightT = 40.0))
        }
        val result = router.route(
            trip = trip,
            vehicle = vehicle,
            effectiveClearanceHeightM = 3.65,
            mode = OptimizeMode.FASTEST,
            tollPreference = TollPreference.ALLOW_SOME,
        )
        assertTrue(result is RouteResult.Blocked)
    }

    @Test
    fun picksFastestSafeRoute() {
        val router = StubGraphHopperRouter {
            listOf(
                RouteCandidate("slow", 5000, 50, maxHeightM = 4.5, maxWeightT = 40.0),
                RouteCandidate("fast", 2000, 200, maxHeightM = 4.5, maxWeightT = 40.0),
            )
        }
        val result = router.route(
            trip = trip,
            vehicle = vehicle,
            effectiveClearanceHeightM = 3.65,
            mode = OptimizeMode.FASTEST,
            tollPreference = TollPreference.ALLOW_SOME,
        )
        assertTrue(result is RouteResult.Ok)
        assertTrue((result as RouteResult.Ok).selected.id == "fast")
    }
}
