package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Test

class OnBoardCargoTest {
    private fun pick(desc: String, height: Double = 3.5) =
        Cargo(desc, height, 2.4, 6.0, 8.0, CargoAction.PICK_UP)

    private fun drop(desc: String, height: Double = 3.5) =
        Cargo(desc, height, 2.4, 6.0, 8.0, CargoAction.DROP_OFF)

    @Test
    fun pickUpAddsCargoOnBoard() {
        val stops = listOf(
            TripStop(id = "1", addressLabel = "Depot", cargo = listOf(pick("crates"))),
        )
        val onBoard = OnBoardCargo.afterStops(stops, throughIndexInclusive = 0)
        assertEquals(1, onBoard.size)
        assertEquals("crates", onBoard[0].description)
    }

    @Test
    fun dropOffRemovesMatchingDescription() {
        val stops = listOf(
            TripStop(id = "1", addressLabel = "Depot", cargo = listOf(pick("crates", 3.5))),
            TripStop(id = "2", addressLabel = "Yard", cargo = listOf(drop("crates", 3.5))),
        )
        val afterPick = OnBoardCargo.afterStops(stops, throughIndexInclusive = 0)
        assertEquals(1, afterPick.size)
        val afterDrop = OnBoardCargo.afterStops(stops, throughIndexInclusive = 1)
        assertEquals(0, afterDrop.size)
    }

    @Test
    fun effectiveClearanceUsesTallestOnBoardPlusBuffer() {
        val stops = listOf(
            TripStop(id = "1", addressLabel = "Depot", cargo = listOf(pick("crates", 3.5))),
        )
        val effective = ClearanceCalculator.effectiveHeightAfterStopM(
            vehicleHeightM = 3.0,
            stops = stops,
            throughIndexInclusive = 0,
            bufferM = 0.15,
        )
        assertEquals(3.65, effective, 1e-9)
    }

    @Test
    fun afterDropOffClearanceFallsBackToVehiclePlusBuffer() {
        val stops = listOf(
            TripStop(id = "1", addressLabel = "Depot", cargo = listOf(pick("crates", 3.5))),
            TripStop(id = "2", addressLabel = "Yard", cargo = listOf(drop("crates", 3.5))),
        )
        val effective = ClearanceCalculator.effectiveHeightAfterStopM(
            vehicleHeightM = 3.0,
            stops = stops,
            throughIndexInclusive = 1,
            bufferM = 0.15,
        )
        assertEquals(3.15, effective, 1e-9)
    }

    @Test
    fun maxAlongTripUsesPeakOnBoardHeight() {
        val stops = listOf(
            TripStop(id = "1", addressLabel = "Depot", cargo = listOf(pick("crates", 3.5))),
            TripStop(id = "2", addressLabel = "Yard", cargo = listOf(drop("crates", 3.5))),
        )
        val peak = ClearanceCalculator.maxEffectiveHeightAlongTripM(
            vehicleHeightM = 3.0,
            stops = stops,
            bufferM = 0.15,
        )
        assertEquals(3.65, peak, 1e-9)
    }
}
