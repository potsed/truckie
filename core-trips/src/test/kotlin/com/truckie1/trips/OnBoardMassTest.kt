package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnBoardMassTest {
    private val pick = TripStop(
        id = "1",
        addressLabel = "Depot",
        cargo = listOf(Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.PICK_UP)),
    )
    private val drop = TripStop(
        id = "2",
        addressLabel = "Yard",
        cargo = listOf(Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.DROP_OFF)),
    )

    @Test
    fun peakOperatingMassIncludesTareAndOnBoardCargo() {
        val peak = OnBoardMass.peakOperatingMassT(tareT = 8.5, stops = listOf(pick, drop))
        assertEquals(16.5, peak, 1e-9)
    }

    @Test
    fun exceedsGvmWhenPeakAboveRating() {
        assertTrue(OnBoardMass.exceedsGvm(tareT = 8.5, gvmT = 15.0, stops = listOf(pick, drop)))
        assertFalse(OnBoardMass.exceedsGvm(tareT = 8.5, gvmT = 26.0, stops = listOf(pick, drop)))
    }
}
