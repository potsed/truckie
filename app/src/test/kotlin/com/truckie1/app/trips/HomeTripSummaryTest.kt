package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.ActiveTourSession
import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTripSummaryTest {
    private fun tourWith(vararg stops: TripStop): ActiveTourSession =
        ActiveTourSession().also { it.replaceStops(stops.toList()) }

    @Test
    fun returnsNullWhenMultiStopFlagOff() {
        val flags = LocalFeatureFlags()
        val session = tourWith(TripStop(id = "1", addressLabel = "Depot"))
        assertNull(HomeTripSummary.text(flags, session))
    }

    @Test
    fun showsStopOverviewWhenFlagOn() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MULTI_STOP to true))
        val session = tourWith(
            TripStop(id = "1", addressLabel = "Depot"),
            TripStop(id = "2", addressLabel = "Yard"),
        )
        val text = HomeTripSummary.text(flags, session)!!
        assertTrue(text.contains("2 stops"))
        assertTrue(text.contains("Depot"))
        assertTrue(text.contains("Yard"))
    }

    @Test
    fun showsOptimizeModeWhenPersisted() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MULTI_STOP to true))
        val session = tourWith(TripStop(id = "1", addressLabel = "Depot"))
        session.lastOptimizeMode = OptimizeMode.FASTEST
        val text = HomeTripSummary.text(flags, session)!!
        assertTrue(text.contains("Fastest"))
    }

    @Test
    fun clearanceUsesOnBoardWhenCargoFlagOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MULTI_STOP to true,
                FlagKeys.STOP_CARGO to true,
            ),
        )
        val session = tourWith(
            TripStop(
                id = "1",
                addressLabel = "Depot",
                cargo = listOf(
                    Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.PICK_UP),
                ),
            ),
        )
        val text = HomeTripSummary.clearanceLine(
            flags = flags,
            session = session,
            vehicleHeightM = 3.0,
            bufferM = 0.15,
        )!!
        assertTrue(text.contains("3.65"))
    }
}
