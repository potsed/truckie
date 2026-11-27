package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Route path from current location (`ff_route_path_from_location`, ATNA-99).
 *
 * When on, [com.truckie1.app.routing.ActiveTourPath] prepends a PathRouter
 * approach leg from the fused fix → route start, then ordered stops → end.
 * Without a fix, geometry falls back to start → stops → end.
 *
 * Defaults **on**. Preview needs [FlagKeys.MAP_ROUTE_POLYLINE]; Navigate needs
 * [FlagKeys.NAVIGATE] + [FlagKeys.NAVIGATE_MULTI_STOP].
 *
 * Gherkin: features/route_path_from_location.feature
 */
object RoutePathFromLocationCatalog {
    const val FLAG_KEY: String = FlagKeys.ROUTE_PATH_FROM_LOCATION

    /** Ship / dogfood default after ATNA-99. */
    const val DEFAULT_ENABLED: Boolean = true

    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    const val usesActiveTourPathApproach: Boolean = true
    const val approachIsPathRouterLeg: Boolean = true
    const val allowsDiyStraightLineApproach: Boolean = false
    const val fallbackWithoutFixIsStartThroughEnd: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION, enabled)
    }

    /**
     * Effective approach fix for geometry: non-null only when the flag is on
     * and a fused fix is available.
     */
    fun approachFix(
        flags: FeatureFlags,
        currentLocation: com.truckie1.routing.LatLngPoint?,
    ): com.truckie1.routing.LatLngPoint? =
        if (isEnabled(flags)) currentLocation else null
}
