package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation_graphhopper.feature —
 * GraphHopper leg composition for multi-stop tours.
 */
class MultiStopTourComposerTest {

    private val vehicle = Vehicle(
        rego = "ABC123",
        nickname = "Rigid",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = 2019,
        heightM = 3.5,
        widthM = 2.5,
        lengthM = 12.5,
        massT = 10.0,
        tareT = 8.5,
        gvmT = 26.0,
        gcmT = 42.5,
        axleCount = 3,
        networkClass = "GML",
    )

    private val depot = TripStop(
        id = "depot",
        addressLabel = "Depot",
        latitude = -27.42,
        longitude = 153.10,
    )
    private val near = TripStop(
        id = "near",
        addressLabel = "Near",
        latitude = -27.43,
        longitude = 153.11,
    )
    private val far = TripStop(
        id = "far",
        addressLabel = "Far",
        latitude = -27.70,
        longitude = 153.25,
    )

    @Test
    fun composesLegsAndPicksShorterTourForFastest() {
        val composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter())
        val result = composer.optimize(
            stops = listOf(depot, far, near),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is OptimizeResult.Ok)
        val ok = result as OptimizeResult.Ok
        assertEquals(listOf("depot", "near", "far"), ok.orderedStops.map { it.id })
        assertNotNull(ok.path)
        assertTrue(ok.path!!.points.size >= 2)
        assertNotNull(ok.customModel.toRequestSnippet())
        assertTrue(ok.customModel.toRequestSnippet().contains("block_under_low_clearance"))
        assertTrue(ok.customModel.toRequestSnippet().contains("over_deck_ok"))
    }

    @Test
    fun blocksTourWhenUnderBridgeConflictsAlongPath() {
        val underLow = RoutingBridgeConstraint(
            id = "low-under",
            latitude = -27.425,
            longitude = 153.105,
            clearanceHeightM = 1.9,
            passage = RoutingBridgePassage.UNDER,
        )
        // Stub interpolates depot→near through the under-bridge corridor.
        val composer = MultiStopTourComposer(
            pathRouter = StubGraphHopperPathRouter(intermediatePoints = 8),
            bridges = listOf(underLow),
            bridgePassageEnabled = true,
        )
        val result = composer.optimize(
            stops = listOf(depot, near),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is OptimizeResult.Blocked)
        val reason = (result as OptimizeResult.Blocked).reason.lowercase()
        assertTrue(reason.contains("bridge") || reason.contains("clearance") || reason.contains("under"))
    }

    @Test
    fun requiresCoordinatesOnStops() {
        val composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter())
        val result = composer.optimize(
            stops = listOf(
                TripStop(id = "a", addressLabel = "A"),
                TripStop(id = "b", addressLabel = "B", latitude = -27.4, longitude = 153.0),
            ),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is OptimizeResult.Blocked)
        assertTrue((result as OptimizeResult.Blocked).reason.lowercase().contains("coord"))
    }
}
