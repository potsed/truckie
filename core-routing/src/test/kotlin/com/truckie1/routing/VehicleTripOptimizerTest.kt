package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripSession
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTripOptimizerTest {
    private val vehicle = Vehicle(
        rego = "ABC123",
        nickname = "Rigid",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = 2019,
        heightM = 3.0,
        widthM = 2.5,
        lengthM = 12.5,
        massT = 10.0,
        tareT = 8.5,
        gvmT = 26.0,
        gcmT = 42.5,
        axleCount = 3,
        networkClass = "GML",
    )

    private val depot = TripStop(id = "1", addressLabel = "Depot")
    private val site = TripStop(
        id = "2",
        addressLabel = "Site",
        cargo = listOf(Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.PICK_UP)),
    )
    private val yard = TripStop(
        id = "3",
        addressLabel = "Yard",
        cargo = listOf(Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.DROP_OFF)),
    )

    @Test
    fun selectsFastestFeasibleTourAndPersistsStopOrder() {
        val session = TripSession(listOf(depot, yard, site))
        val optimizer = VehicleTripOptimizer(
            candidatesForOrder = { ordered ->
                // Depot → Site → Yard is faster; Depot → Yard → Site is cheaper but slower.
                when (ordered.map { it.addressLabel }) {
                    listOf("Depot", "Site", "Yard") -> listOf(
                        RouteCandidate("dsy-fast", 2000, 200, maxHeightM = 5.0, maxWeightT = 40.0),
                    )
                    listOf("Depot", "Yard", "Site") -> listOf(
                        RouteCandidate("dys-cheap", 5000, 50, maxHeightM = 5.0, maxWeightT = 40.0),
                    )
                    else -> listOf(
                        RouteCandidate("other", 9000, 900, maxHeightM = 5.0, maxWeightT = 40.0),
                    )
                }
            },
        )

        val result = optimizer.optimize(
            stops = session.stops(),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )

        assertTrue(result is OptimizeResult.Ok)
        val ok = result as OptimizeResult.Ok
        assertEquals(listOf("Depot", "Site", "Yard"), ok.orderedStops.map { it.addressLabel })
        assertEquals("dsy-fast", ok.route.id)

        session.replaceStops(ok.orderedStops)
        session.lastOptimizeMode = ok.mode
        assertEquals(listOf("Depot", "Site", "Yard"), session.stops().map { it.addressLabel })
        assertEquals(OptimizeMode.FASTEST, session.lastOptimizeMode)
    }

    @Test
    fun selectsCheapestFeasibleTour() {
        val optimizer = VehicleTripOptimizer(
            candidatesForOrder = { ordered ->
                when (ordered.map { it.addressLabel }) {
                    listOf("Depot", "Site", "Yard") -> listOf(
                        RouteCandidate("dsy-fast", 2000, 200, maxHeightM = 5.0, maxWeightT = 40.0),
                    )
                    listOf("Depot", "Yard", "Site") -> listOf(
                        RouteCandidate("dys-cheap", 5000, 50, maxHeightM = 5.0, maxWeightT = 40.0),
                    )
                    else -> emptyList()
                }
            },
        )

        val result = optimizer.optimize(
            stops = listOf(depot, site, yard),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.CHEAPEST,
        )

        assertTrue(result is OptimizeResult.Ok)
        val ok = result as OptimizeResult.Ok
        assertEquals(listOf("Depot", "Yard", "Site"), ok.orderedStops.map { it.addressLabel })
        assertEquals("dys-cheap", ok.route.id)
    }

    @Test
    fun blocksWhenNoTourSatisfiesClearanceAndWeight() {
        val optimizer = VehicleTripOptimizer(
            candidatesForOrder = {
                listOf(RouteCandidate("low", 1000, 100, maxHeightM = 3.4, maxWeightT = 40.0))
            },
        )

        val result = optimizer.optimize(
            stops = listOf(depot, site, yard),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )

        assertTrue(result is OptimizeResult.Blocked)
        val reason = (result as OptimizeResult.Blocked).reason
        assertTrue(reason.lowercase().contains("clearance") || reason.contains("3.65"))
    }

    @Test
    fun blocksWhenOnBoardMassExceedsGvmEvenIfRouteIsTallEnough() {
        val heavy = site.copy(
            cargo = listOf(Cargo("steel", 2.0, 2.0, 4.0, 20.0, CargoAction.PICK_UP)),
        )
        val optimizer = VehicleTripOptimizer(
            candidatesForOrder = {
                listOf(RouteCandidate("ok-bridge", 1000, 100, maxHeightM = 5.0, maxWeightT = 40.0))
            },
        )

        val result = optimizer.optimize(
            stops = listOf(depot, heavy, yard.copy(cargo = listOf(Cargo("steel", 2.0, 2.0, 4.0, 20.0, CargoAction.DROP_OFF)))),
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )

        assertTrue(result is OptimizeResult.Blocked)
        assertTrue((result as OptimizeResult.Blocked).reason.lowercase().contains("gvm") ||
            result.reason.lowercase().contains("mass"))
    }

    @Test
    fun tollPreferenceDefaultsToAllowAllPassthrough() {
        val model = GraphHopperCustomModelStub.from(
            vehicle = vehicle,
            requiredClearanceHeightM = 3.65,
            requiredMassT = 16.5,
        )
        assertEquals(TollPreference.ALLOW_ALL, model.tollPreference)
    }
}
