package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Catalog for create-route start + end (`ff_route_start_end`).
 */
object RouteStartEndCatalog {
    const val FLAG: String = FlagKeys.ROUTE_START_END

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ROUTE_START_END)

    fun requiresEndpointsOnCreate(flags: FeatureFlags): Boolean = isEnabled(flags)
}
