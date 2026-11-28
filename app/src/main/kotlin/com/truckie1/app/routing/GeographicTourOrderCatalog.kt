package com.truckie1.app.routing

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.TripStop

/**
 * Geographic tour stop order (`ff_geographic_tour_order`, ATNA-120).
 *
 * When on with Optimise / ActiveTourPath, PathRouter tours keep start and end
 * anchored and choose closer-first / shortest feasible intermediate order under
 * bridge/weight/toll constraints — not raw input zig-zag.
 *
 * Synthetic waypoint ids match [ActiveTourPath.tourStopsFrom].
 *
 * Gherkin: features/geographic_tour_order.feature
 */
object GeographicTourOrderCatalog {
    const val FLAG_KEY: String = FlagKeys.GEOGRAPHIC_TOUR_ORDER

    const val START_ID: String = "start"
    const val END_ID: String = "end"

    /** Defaults on for select-user / debug dogfood (same family as navigate multi-stop). */
    const val DEFAULT_ENABLED: Boolean = true
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.GEOGRAPHIC_TOUR_ORDER)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.GEOGRAPHIC_TOUR_ORDER, enabled)
    }

    /** Drop synthetic start/end waypoints before persisting Route.stops. */
    fun intermediateStops(ordered: List<TripStop>): List<TripStop> =
        ordered.filter { it.id != START_ID && it.id != END_ID }
}
