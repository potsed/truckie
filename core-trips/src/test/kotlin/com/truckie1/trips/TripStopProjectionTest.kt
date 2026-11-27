package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: active_tour.feature — TripStop is a projection of RouteStop.
 */
class TripStopProjectionTest {

    @Test
    fun toTripStopCopiesIdentityCoordsAndCargo() {
        val routeStop = RouteStop(
            id = "s1",
            addressLabel = "42 South St",
            latitude = -27.41,
            longitude = 153.15,
            kind = StopKind.PICKUP_LOAD,
            order = 0,
            cargo = listOf(
                Cargo("Crates", 3.8, 2.2, 3.0, 2.0, CargoAction.PICK_UP),
            ),
        )

        val trip = TripStopProjection.toTripStop(routeStop)

        assertEquals("s1", trip.id)
        assertEquals("42 South St", trip.addressLabel)
        assertEquals(-27.41, trip.latitude!!, 0.0001)
        assertEquals(153.15, trip.longitude!!, 0.0001)
        assertEquals(1, trip.cargo.size)
        assertEquals(CargoAction.PICK_UP, trip.cargo.first().action)
    }

    @Test
    fun toRouteStopPreservesKindWhenProvided() {
        val trip = TripStop(
            id = "s2",
            addressLabel = "Yard",
            latitude = -27.5,
            longitude = 153.1,
            cargo = listOf(
                Cargo("Pallets", 3.2, 2.4, 4.0, 2.5, CargoAction.DROP_OFF),
            ),
        )

        val routeStop = TripStopProjection.toRouteStop(
            stop = trip,
            order = 3,
            kind = StopKind.DROP_OFF_UNLOAD,
        )

        assertEquals(3, routeStop.order)
        assertEquals(StopKind.DROP_OFF_UNLOAD, routeStop.kind)
        assertEquals(trip.cargo, routeStop.cargo)
    }

    @Test
    fun inferKindFromCargoActions() {
        val pickup = TripStop(
            id = "p",
            addressLabel = "Pickup",
            cargo = listOf(Cargo("Crates", 3.0, 2.0, 3.0, 1.0, CargoAction.PICK_UP)),
        )
        val drop = TripStop(
            id = "d",
            addressLabel = "Drop",
            cargo = listOf(Cargo("Crates", 3.0, 2.0, 3.0, 1.0, CargoAction.DROP_OFF)),
        )
        val empty = TripStop(id = "e", addressLabel = "Empty")

        assertEquals(StopKind.PICKUP_LOAD, TripStopProjection.inferKind(pickup))
        assertEquals(StopKind.DROP_OFF_UNLOAD, TripStopProjection.inferKind(drop))
        assertEquals(StopKind.DROP_OFF_UNLOAD, TripStopProjection.inferKind(empty))
    }

    @Test
    fun roundTripPreservesStopIdsOrderAndCargo() {
        val original = listOf(
            RouteStop(
                id = "a",
                addressLabel = "A",
                latitude = -27.4,
                longitude = 153.0,
                kind = StopKind.PICKUP_LOAD,
                order = 0,
                cargo = listOf(Cargo("X", 1.0, 1.0, 1.0, 0.5, CargoAction.PICK_UP)),
            ),
            RouteStop(
                id = "b",
                addressLabel = "B",
                latitude = -27.5,
                longitude = 153.1,
                kind = StopKind.DROP_OFF_UNLOAD,
                order = 1,
            ),
        )
        val projected = TripStopProjection.toTripStops(original)
        val restored = TripStopProjection.toRouteStops(projected, previous = original)

        assertEquals(original.map { it.id }, restored.map { it.id })
        assertEquals(original.map { it.kind }, restored.map { it.kind })
        assertEquals(original.map { it.cargo }, restored.map { it.cargo })
        assertTrue(restored.mapIndexed { i, s -> s.order == i }.all { it })
    }
}
