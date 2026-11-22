package com.truckie1.routing

import com.truckie1.trips.OptimizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeRouteSelectorTest {
    private val selector = SafeRouteSelector()

    @Test
    fun picksFastestAmongHeightAndWeightSafeCandidates() {
        val result = selector.select(
            candidates = listOf(
                RouteCandidate("slow-cheap", 5000, 50, maxHeightM = 4.5, maxWeightT = 40.0),
                RouteCandidate("fast-pricey", 2000, 200, maxHeightM = 4.5, maxWeightT = 40.0),
                RouteCandidate("fastest-but-low", 1000, 80, maxHeightM = 3.0, maxWeightT = 40.0),
            ),
            requiredClearanceHeightM = 3.65,
            requiredMassT = 22.5,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is RouteResult.Ok)
        assertEquals("fast-pricey", (result as RouteResult.Ok).selected.id)
    }

    @Test
    fun picksCheapestAmongSafeCandidates() {
        val result = selector.select(
            candidates = listOf(
                RouteCandidate("slow-cheap", 5000, 50, maxHeightM = 4.5, maxWeightT = 40.0),
                RouteCandidate("fast-pricey", 2000, 200, maxHeightM = 4.5, maxWeightT = 40.0),
            ),
            requiredClearanceHeightM = 3.65,
            requiredMassT = 22.5,
            mode = OptimizeMode.CHEAPEST,
        )
        assertTrue(result is RouteResult.Ok)
        assertEquals("slow-cheap", (result as RouteResult.Ok).selected.id)
    }

    @Test
    fun blocksWhenNoCandidateMeetsClearanceOrMass() {
        val result = selector.select(
            candidates = listOf(
                RouteCandidate("low", 1000, 100, maxHeightM = 3.4, maxWeightT = 40.0),
                RouteCandidate("light", 1200, 100, maxHeightM = 5.0, maxWeightT = 10.0),
            ),
            requiredClearanceHeightM = 3.65,
            requiredMassT = 22.5,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is RouteResult.Blocked)
        val reason = (result as RouteResult.Blocked).reason
        assertTrue(reason.contains("3.65") || reason.contains("clearance") || reason.contains("mass"))
    }
}
