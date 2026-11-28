package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation_graphhopper.feature —
 * GraphHopper leg composition for multi-stop tours (ATNA-68/71).
 * Legacy VehicleTripOptimizer coverage moved here.
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
    fun composesLegsAndPicksShorterTourForFastest() = runBlocking {
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
    fun picksLongerCheaperGeometryWhenModeIsCheapest() = runBlocking {
        val composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter())
        val fastest = composer.optimize(
            stops = listOf(depot, far, near),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        ) as OptimizeResult.Ok
        val cheapest = composer.optimize(
            stops = listOf(depot, far, near),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.CHEAPEST,
        ) as OptimizeResult.Ok
        assertEquals(fastest.orderedStops.map { it.id }, cheapest.orderedStops.map { it.id })
        assertTrue(cheapest.route.costCents >= 1L)
    }

    @Test
    fun detoursViaPathRouterWhenUnderBridgeConflictsAlongPath() = runBlocking {
        val underLow = RoutingBridgeConstraint(
            id = "low-under",
            latitude = -27.425,
            longitude = 153.105,
            clearanceHeightM = 1.9,
            passage = RoutingBridgePassage.UNDER,
        )
        val composer = MultiStopTourComposer(
            pathRouter = StubGraphHopperPathRouter(intermediatePoints = 8),
            bridges = listOf(underLow),
            bridgePassageEnabled = true,
            pathBlockDetourEnabled = true,
        )
        val result = composer.optimize(
            stops = listOf(depot, near),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(
            "PathRouter detour must keep a drawable tour; got $result",
            result is OptimizeResult.Ok,
        )
        val path = (result as OptimizeResult.Ok).path
        assertNotNull(path)
        assertTrue(path!!.points.size >= 2)
        assertTrue(PathBlockDetour.usesPathRouter)
        assertFalse(PathBlockDetour.usesHandRolledPolylineRule)
    }

    @Test
    fun deliveryStopBesideUnderBridgeStillComposesOrderedPath() = runBlocking {
        // Demo Wynnum residential stop ~32 m from Colina St (1.9 m) — stop markers
        // show, so PathRouter geometry must still resolve for LineManager.
        val wynnum = TripStop(
            id = "wynnum",
            addressLabel = "8 Colina Street, Wynnum QLD 4178",
            latitude = -27.44395,
            longitude = 153.16905,
        )
        val hemmant = TripStop(
            id = "hemmant",
            addressLabel = "Hemmant",
            latitude = -27.4502,
            longitude = 153.1295,
        )
        val colina = RoutingBridgeConstraint(
            id = "qld-qr-wynnum-colina-street-berrima-street",
            latitude = -27.443741,
            longitude = 153.168832,
            clearanceHeightM = 1.9,
            passage = RoutingBridgePassage.UNDER,
        )
        val composer = MultiStopTourComposer(
            pathRouter = StubGraphHopperPathRouter(),
            bridges = listOf(colina),
            bridgePassageEnabled = true,
        )
        val result = composer.composeOrdered(
            stops = listOf(hemmant, wynnum, depot),
            vehicle = vehicle,
            bufferM = 0.15,
        )
        assertTrue(
            "Expected Ready path when only stop proximity hits Colina; got $result",
            result is OptimizeResult.Ok,
        )
        val path = (result as OptimizeResult.Ok).path
        assertNotNull(path)
        assertTrue(path!!.points.size >= 2)
    }

    @Test
    fun blocksWhenOnBoardMassExceedsGvm() = runBlocking {
        val heavyPick = near.copy(
            cargo = listOf(Cargo("steel", 2.0, 2.0, 4.0, 20.0, CargoAction.PICK_UP)),
        )
        val heavyDrop = far.copy(
            cargo = listOf(Cargo("steel", 2.0, 2.0, 4.0, 20.0, CargoAction.DROP_OFF)),
        )
        val composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter())
        val result = composer.optimize(
            stops = listOf(depot, heavyPick, heavyDrop),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is OptimizeResult.Blocked)
        val reason = (result as OptimizeResult.Blocked).reason.lowercase()
        assertTrue(reason.contains("gvm") || reason.contains("mass"))
    }

    @Test
    fun requiresCoordinatesOnStops() = runBlocking {
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

    @Test
    fun tollPreferenceDefaultsToAllowAllOnCustomModel() {
        val model = GraphHopperCustomModelStub.from(
            vehicle = vehicle,
            requiredClearanceHeightM = 3.65,
            requiredMassT = 16.5,
        )
        assertEquals(TollPreference.ALLOW_ALL, model.tollPreference)
    }
}
