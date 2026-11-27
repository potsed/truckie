package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Active tour single SoT (`ff_active_tour`, ATNA-86 / P1-1).
 *
 * Map-home create-route, Plan Trip, Optimise, and demo share one
 * [com.truckie1.trips.ActiveTourSession] (or read-only [ReadOnlyActiveTour]
 * in demo). [com.truckie1.trips.TripStop] is a projection of
 * [com.truckie1.trips.RouteStop] — there is no parallel Trip stop list.
 *
 * Defaults **on**. The legacy dual RouteSession + TripSession path is deleted.
 *
 * Gherkin: features/active_tour.feature
 */
object ActiveTourCatalog {
    const val FLAG_KEY: String = FlagKeys.ACTIVE_TOUR

    /** Ship / dogfood default after ATNA-86. */
    const val DEFAULT_ENABLED: Boolean = true

    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.ACTIVE_TOUR)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.ACTIVE_TOUR, enabled)
    }

    /** True when [AppDataSource.routes] and [AppDataSource.trips] are the same instance. */
    fun sharesSingleTour(source: AppDataSource): Boolean =
        source.routes === source.trips
}
