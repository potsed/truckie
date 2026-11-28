package com.truckie1.app.trips

import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.RoutePath

/**
 * Formats optimised stop order / path source for Trip stops UI.
 * Kept out of MapHomeScreen so drawer PRs stay merge-friendly.
 */
object OptimizeResultSummary {
    fun orderLabels(result: OptimizeResult.Ok): String =
        result.orderedStops.mapIndexed { index, stop ->
            "${index + 1}. ${stop.addressLabel}"
        }.joinToString(separator = " → ")

    fun pathSourceLabel(result: OptimizeResult.Ok): String {
        val source = result.path?.source ?: RoutePath.SOURCE_STUB
        return when (source) {
            RoutePath.SOURCE_VALHALLA -> "Valhalla live"
            else ->
                "Stub (degraded — not road network; set VALHALLA_API_KEY or VALHALLA_BASE_URL)"
        }
    }

    fun combined(result: OptimizeResult.Ok): String {
        val mode = result.mode.name.lowercase().replaceFirstChar { it.titlecase() }
        return "$mode · ${result.route.durationSeconds}s · ${pathSourceLabel(result)}\n" +
            orderLabels(result)
    }
}
