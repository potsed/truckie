package com.truckie1.routing

import com.truckie1.trips.TripStop

/**
 * Candidate stop orders for [MultiStopTourComposer] (ATNA-120).
 *
 * Legacy: fix the first stop as origin; permute the rest (may move a route
 * [end] into the middle when ActiveTourPath waypoints are passed).
 *
 * Geographic (`ff_geographic_tour_order`): fix origin **and** destination;
 * permute only intermediate stops so tours stay start → … → end.
 */
object TourCandidateOrders {

    fun candidates(
        stops: List<TripStop>,
        anchorDestination: Boolean,
    ): List<List<TripStop>> {
        if (stops.size <= 1) return listOf(stops)
        if (!anchorDestination || stops.size == 2) {
            return originOnly(stops)
        }
        return originAndDestination(stops)
    }

    /** First stop fixed; remaining permuted (legacy Optimise). */
    fun originOnly(stops: List<TripStop>): List<List<TripStop>> {
        if (stops.size <= 1) return listOf(stops)
        val origin = stops.first()
        val rest = stops.drop(1)
        return permutations(rest).map { perm -> listOf(origin) + perm }
    }

    /** First and last fixed; middle permuted (geographic tour). */
    fun originAndDestination(stops: List<TripStop>): List<List<TripStop>> {
        if (stops.size <= 2) return listOf(stops)
        val origin = stops.first()
        val destination = stops.last()
        val middle = stops.drop(1).dropLast(1)
        if (middle.isEmpty()) return listOf(stops)
        return permutations(middle).map { perm -> listOf(origin) + perm + destination }
    }

    private fun <T> permutations(items: List<T>): List<List<T>> {
        if (items.size <= 1) return listOf(items)
        val result = mutableListOf<List<T>>()
        for (i in items.indices) {
            val head = items[i]
            val others = items.filterIndexed { index, _ -> index != i }
            for (tail in permutations(others)) {
                result += listOf(head) + tail
            }
        }
        return result
    }
}
