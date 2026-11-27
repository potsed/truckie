package com.truckie1.routing

import com.truckie1.trips.TourOptimizeGoals

/**
 * Picks among feasible tour / route candidates using Fastest + Cost Effective
 * **checkbox** goals (both on by default = AND).
 *
 * AND: prefer lower combined duration×cost (fast and cost-effective together),
 * not a single-axis radio. Single-checkbox modes fall back to that axis only.
 */
object TourPathSelector {

    /**
     * Combined AND score — lower is better. Used when both checkboxes are on
     * (product default for map route + Optimize).
     */
    fun andScore(durationSeconds: Long, costCents: Long): Long =
        durationSeconds.coerceAtLeast(1L) * costCents.coerceAtLeast(1L)

    fun andScore(candidate: RouteCandidate): Long =
        andScore(candidate.durationSeconds, candidate.costCents)

    fun isBetter(
        next: RouteCandidate,
        current: RouteCandidate,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
    ): Boolean = when {
        goals.preferFastest && goals.preferCostEffective ->
            andScore(next) < andScore(current)
        goals.preferFastest && !goals.preferCostEffective ->
            next.durationSeconds < current.durationSeconds
        !goals.preferFastest && goals.preferCostEffective ->
            next.costCents < current.costCents
        else ->
            // Neither checkbox: still prefer AND as a stable deterministic tie-break.
            andScore(next) < andScore(current)
    }

    fun <T> pickBetter(
        current: T?,
        next: T,
        candidateOf: (T) -> RouteCandidate,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
    ): T {
        if (current == null) return next
        return if (isBetter(candidateOf(next), candidateOf(current), goals)) next else current
    }

    fun select(
        candidates: List<RouteCandidate>,
        goals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT,
    ): RouteCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.reduce { best, next ->
            if (isBetter(next, best, goals)) next else best
        }
    }
}
