package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Test

class ClearanceCalculatorTest {
    @Test
    fun clearanceIsMaxVehicleOrTallestCargoPlusBuffer() {
        val effective = ClearanceCalculator.effectiveHeightM(
            vehicleHeightM = 3.0,
            cargoHeightsM = listOf(2.1, 3.5, 3.2),
            bufferM = 0.15,
        )
        assertEquals(3.65, effective, 1e-9)
    }

    @Test
    fun vehicleTallerThanCargoUsesVehiclePlusBuffer() {
        val effective = ClearanceCalculator.effectiveHeightM(
            vehicleHeightM = 4.0,
            cargoHeightsM = listOf(3.5),
            bufferM = 0.2,
        )
        assertEquals(4.2, effective, 1e-9)
    }

    @Test
    fun emptyCargoUsesVehiclePlusBuffer() {
        val effective = ClearanceCalculator.effectiveHeightM(
            vehicleHeightM = 3.0,
            cargoHeightsM = emptyList(),
            bufferM = 0.15,
        )
        assertEquals(3.15, effective, 1e-9)
    }

    @Test
    fun bufferClampedToAllowedRange() {
        assertEquals(0.15, ClearanceBuffer.clamp(0.01), 1e-9)
        assertEquals(1.0, ClearanceBuffer.clamp(2.0), 1e-9)
        val low = ClearanceCalculator.effectiveHeightM(3.0, listOf(3.5), 0.01)
        assertEquals(3.65, low, 1e-9)
        val high = ClearanceCalculator.effectiveHeightM(3.0, listOf(3.5), 5.0)
        assertEquals(4.5, high, 1e-9)
    }

    @Test
    fun derivesCargoHeightsFromStops() {
        val stops = listOf(
            TripStop(
                id = "1",
                addressLabel = "Depot",
                cargo = listOf(
                    Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.PICK_UP),
                ),
            ),
            TripStop(
                id = "2",
                addressLabel = "Yard",
                cargo = listOf(
                    Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.DROP_OFF),
                ),
            ),
        )
        val effective = ClearanceCalculator.effectiveHeightForTripM(
            vehicleHeightM = 3.0,
            stops = stops,
            bufferM = 0.15,
        )
        assertEquals(3.65, effective, 1e-9)
    }
}
