package com.truckie1.routing

import com.truckie1.trips.TourOptimizeGoals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fastest + Cost Effective are checkboxes (both on by default = AND).
 */
class TourPathSelectorTest {

    private fun candidate(
        id: String,
        durationSeconds: Long,
        costCents: Long,
    ) = RouteCandidate(
        id = id,
        durationSeconds = durationSeconds,
        costCents = costCents,
        maxHeightM = 50.0,
        maxWeightT = 50.0,
    )

    @Test
    fun defaultGoalsPreferAndScoreNotSingleAxis() {
        val goals = TourOptimizeGoals.DEFAULT
        assertTrue(goals.preferFastest)
        assertTrue(goals.preferCostEffective)

        val fastExpensive = candidate("fast-exp", durationSeconds = 100, costCents = 500)
        val slowerCheap = candidate("slow-cheap", durationSeconds = 120, costCents = 50)
        // AND score: 100*500=50000 vs 120*50=6000 → slowerCheap wins (both axes together).
        assertTrue(TourPathSelector.isBetter(slowerCheap, fastExpensive, goals))
        assertFalse(TourPathSelector.isBetter(fastExpensive, slowerCheap, goals))
    }

    @Test
    fun fastestOnlyIgnoresCost() {
        val goals = TourOptimizeGoals(preferFastest = true, preferCostEffective = false)
        val fastExpensive = candidate("fast-exp", durationSeconds = 100, costCents = 500)
        val slowerCheap = candidate("slow-cheap", durationSeconds = 120, costCents = 50)
        assertTrue(TourPathSelector.isBetter(fastExpensive, slowerCheap, goals))
    }

    @Test
    fun costEffectiveOnlyIgnoresDuration() {
        val goals = TourOptimizeGoals(preferFastest = false, preferCostEffective = true)
        val fastExpensive = candidate("fast-exp", durationSeconds = 100, costCents = 500)
        val slowerCheap = candidate("slow-cheap", durationSeconds = 120, costCents = 50)
        assertTrue(TourPathSelector.isBetter(slowerCheap, fastExpensive, goals))
    }

    @Test
    fun selectUsesAndByDefault() {
        val picked = TourPathSelector.select(
            listOf(
                candidate("a", 100, 500),
                candidate("b", 120, 50),
                candidate("c", 200, 200),
            ),
        )
        assertEquals("b", picked!!.id)
    }
}
