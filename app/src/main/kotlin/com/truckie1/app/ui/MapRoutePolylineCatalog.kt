package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.TourOptimizeGoals

/**
 * Active route polyline on MapLibre home (`ff_map_route_polyline`).
 *
 * Geometry comes from [PathRouter] / [com.truckie1.routing.MultiStopTourComposer]
 * only — never a DIY straight-line between stops. Stub geometry only when the
 * stub PathRouter is the active factory (same as Navigate / Optimize).
 *
 * Constraints match Optimize/Navigate: hard under-passage low bridges
 * (height+buffer) + weight; soft residential; over-deck OK; toll prefs when
 * `ff_tolls`. Shared [MapLineBind] / [MapRoutePathAnnotations] LineManager path.
 *
 * Tour selection: [TourOptimizeGoals] checkboxes (Fastest + Cost Effective),
 * both on by default = AND.
 *
 * Out of scope: satellite peek. Gherkin: features/map_route_polyline.feature.
 */
object MapRoutePolylineCatalog {
    const val FLAG_KEY: String = FlagKeys.MAP_ROUTE_POLYLINE

    const val usesLineManager: Boolean = true
    const val usesSharedLineBindPath: Boolean = true
    const val sharesStyleLifecycleWithNavigate: Boolean = true

    const val usesPathRouterGeometry: Boolean = true
    const val prefersOptimisedTourGeometryWhenAvailable: Boolean = true
    const val allowsDiyStraightLineBetweenStops: Boolean = false
    const val stubGeometryOnlyViaActivePathRouterFactory: Boolean = true

    const val hardBlocksUnderPassageLowBridges: Boolean = true
    const val hardBlocksWeightLimits: Boolean = true
    const val softResidentialPreferenceOnly: Boolean = true
    const val overDeckPassageOk: Boolean = true
    const val appliesTollPrefsWhenFlagged: Boolean = true
    const val usesSamePathRouterAsOptimizeAndNavigate: Boolean = true
    const val usesBridgePassagePolicyAndPackBridges: Boolean = true

    /** Fastest + Cost Effective checkboxes, both on by default (AND). */
    val defaultTourGoals: TourOptimizeGoals = TourOptimizeGoals.DEFAULT

    const val implementsSatellitePeek: Boolean = false

    val LINE_COLOR_HEX: String = MapRoutePathAnnotations.LINE_COLOR_HEX
    val LINE_WIDTH: Float = MapRoutePathAnnotations.LINE_WIDTH

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE)

    fun shouldShow(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled && isEnabled(flags)

    fun appliesTollPreference(flags: FeatureFlags): Boolean =
        appliesTollPrefsWhenFlagged && flags.isEnabled(FlagKeys.TOLLS)
}
