package com.truckie1.routing

import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/geographic_tour_order.feature —
 * destination-anchored candidate orders (ATNA-120).
 */
class TourCandidateOrdersTest {

    private fun stop(id: String) = TripStop(
        id = id,
        addressLabel = id,
        latitude = -27.0,
        longitude = 153.0,
    )

    @Test
    fun geographicCandidatesKeepStartAndEndAnchored() {
        val stops = listOf(stop("start"), stop("a"), stop("b"), stop("end"))
        val candidates = TourCandidateOrders.candidates(stops, anchorDestination = true)
        assertEquals(2, candidates.size)
        for (order in candidates) {
            assertEquals("start", order.first().id)
            assertEquals("end", order.last().id)
            assertEquals(setOf("a", "b"), order.drop(1).dropLast(1).map { it.id }.toSet())
        }
    }

    @Test
    fun legacyOriginOnlyMayMoveEndIntoMiddle() {
        val stops = listOf(stop("start"), stop("a"), stop("end"))
        val candidates = TourCandidateOrders.candidates(stops, anchorDestination = false)
        assertTrue(candidates.any { it.map { s -> s.id } == listOf("start", "end", "a") })
        assertTrue(candidates.any { it.map { s -> s.id } == listOf("start", "a", "end") })
    }
}
