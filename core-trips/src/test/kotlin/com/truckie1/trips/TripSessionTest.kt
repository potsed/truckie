package com.truckie1.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: multi_stop_cargo.feature + active_tour.feature —
 * Trip stop list behaviour via [ActiveTourSession] (legacy TripSession deleted).
 */
class TripSessionTest {
    @Test
    fun addReorderAndDeleteStops() {
        val session = ActiveTourSession()
        session.addStop(TripStop(id = "1", addressLabel = "Depot"))
        session.addStop(TripStop(id = "2", addressLabel = "Site A"))
        session.addStop(TripStop(id = "3", addressLabel = "Yard"))

        session.moveStop(fromIndex = 2, toIndex = 1)
        assertEquals(listOf("Depot", "Yard", "Site A"), session.stops().map { it.addressLabel })

        session.removeStop("2")
        assertEquals(listOf("Depot", "Yard"), session.stops().map { it.addressLabel })
    }

    @Test
    fun replaceStopsPreservesOrder() {
        val session = ActiveTourSession()
        session.replaceStops(
            listOf(
                TripStop(id = "a", addressLabel = "A"),
                TripStop(id = "b", addressLabel = "B"),
            ),
        )
        assertEquals(2, session.stops().size)
        assertEquals("A", session.stops().first().addressLabel)
    }

    @Test
    fun cargoSummaryListsPickAndDropCounts() {
        val stop = TripStop(
            id = "1",
            addressLabel = "Depot",
            cargo = listOf(
                Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.PICK_UP),
                Cargo("beams", 2.0, 1.0, 4.0, 2.0, CargoAction.PICK_UP),
                Cargo("crates", 3.5, 2.4, 6.0, 8.0, CargoAction.DROP_OFF),
            ),
        )
        val summary = TripStopSummary.cargoLine(stop)
        assertTrue(summary.contains("pick 2"))
        assertTrue(summary.contains("drop 1"))
    }
}
