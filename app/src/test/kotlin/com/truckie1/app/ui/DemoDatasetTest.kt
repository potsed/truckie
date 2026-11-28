package com.truckie1.app.ui

import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/demo_mode.feature — demo depot, stops, vehicle (ATNA-56).
 */
class DemoDatasetTest {

    @Test
    fun depotIsDhlBoroniaRoadNearAirport() {
        val depot = DemoDataset.depot
        assertTrue(depot.addressLabel.contains("DHL", ignoreCase = true))
        assertTrue(depot.addressLabel.contains("Boronia", ignoreCase = true))
        assertTrue(
            depot.addressLabel.contains("Brisbane Airport", ignoreCase = true) ||
                depot.addressLabel.contains("Airport", ignoreCase = true),
        )
        assertEquals(-27.419949, depot.latitude, 0.001)
        assertEquals(153.099544, depot.longitude, 0.001)
    }

    @Test
    fun sixStopsTwoPickupsFourDeliveriesWithOneResidential() {
        val stops = DemoDataset.stops
        assertEquals(6, stops.size)
        assertEquals(2, stops.count { it.kind == StopKind.PICKUP_LOAD })
        assertEquals(4, stops.count { it.kind == StopKind.DROP_OFF_UNLOAD })
        assertEquals(1, stops.count { it.areaKind == DemoStopAreaKind.RESIDENTIAL })
        assertEquals(5, stops.count { it.areaKind == DemoStopAreaKind.INDUSTRIAL })
        assertTrue(
            stops.filter { it.areaKind == DemoStopAreaKind.INDUSTRIAL }
                .any { it.suburb.contains("Yatala", ignoreCase = true) },
        )
        assertTrue(
            stops.filter { it.areaKind == DemoStopAreaKind.INDUSTRIAL }
                .any { it.suburb.contains("Lytton", ignoreCase = true) },
        )
    }

    @Test
    fun atLeastOneStopIsNearVerifiedLowBridge() {
        val near = DemoDataset.stopsNearLowBridge()
        assertTrue(near.isNotEmpty())
        near.forEach { stop ->
            assertTrue(stop.nearLowBridge)
            assertTrue(
                DemoDataset.distanceToNearLowBridgeM(stop) <= DemoDataset.NEAR_LOW_BRIDGE_MAX_M,
            )
        }
        // 1.9 m Colina St underbridge conflicts with 3.5 m demo vehicle (+ buffer).
        assertEquals(1.9, DemoDataset.NEAR_LOW_BRIDGE_CLEARANCE_M, 0.001)
        assertTrue(DemoDataset.NEAR_LOW_BRIDGE_CLEARANCE_M < DemoDataset.vehicle.heightM)
        assertTrue(
            near.any { it.areaKind == DemoStopAreaKind.RESIDENTIAL },
        )
    }

    @Test
    fun demoVehicleIsThreePointFiveMetresHigh() {
        val vehicle = DemoDataset.vehicle
        assertEquals(3.5, vehicle.heightM, 0.001)
        assertTrue(DemoDataset.isDemoVehicleRego(vehicle.rego))
        assertFalse(DemoDataset.isDemoVehicleRego("ABC123"))
        assertFalse(vehicle.nickname.contains("demo", ignoreCase = true))
        assertFalse(vehicle.rego.contains("demo", ignoreCase = true))
    }

    @Test
    fun buildRouteUsesDepotAsStartAndEndWithSixStops() {
        val route = DemoDataset.buildRoute()
        assertEquals(DemoDataset.depot, route.start)
        assertEquals(DemoDataset.depot, route.end)
        assertTrue(route.isRoundTrip)
        assertEquals(6, route.stops.size)
        assertEquals(2, route.stops.count { it.kind == StopKind.PICKUP_LOAD })
        assertEquals(4, route.stops.count { it.kind == StopKind.DROP_OFF_UNLOAD })
    }

    @Test
    fun seedOrderIsGeographicNotFarthestFirst() {
        // ATNA-120: demo is read-only so Optimise cannot persist; seed must not
        // zig-zag north → far Yatala → north again as stop #2.
        val first = DemoDataset.stops.first()
        assertFalse(first.suburb.contains("Yatala", ignoreCase = true))
        val yatalaIndex = DemoDataset.stops.indexOfFirst {
            it.suburb.contains("Yatala", ignoreCase = true)
        }
        assertTrue(yatalaIndex > 1)
    }

    @Test
    fun tripStopsIncludeCargoAndCoordsForReadOnlyDogfood() {
        val trips = DemoDataset.buildTripStops()
        assertEquals(6, trips.size)
        assertTrue(trips.all { it.latitude != null && it.longitude != null })
        assertTrue(trips.count { it.cargo.any { c -> c.action.name == "PICK_UP" } } >= 2)
        assertTrue(trips.count { it.cargo.any { c -> c.action.name == "DROP_OFF" } } >= 2)
        assertTrue(DemoDataset.supportsReadOnlyDogfood())
    }
}
