package com.truckie1.app.routing

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.routing.PathBlockDetour

/**
 * Alternate around path blocks via PathRouter (`ff_path_block_detour`, ATNA-102).
 *
 * Defaults **on**. Production design: live Valhalla truck costing / exclude
 * polygons. Never erase the tour polyline when PathRouter can still draw.
 *
 * Gherkin: features/path_block_detour.feature
 */
object PathBlockDetourCatalog {
    const val FLAG_KEY: String = FlagKeys.PATH_BLOCK_DETOUR

    const val DEFAULT_ENABLED: Boolean = true
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    val usesPathRouter: Boolean = PathBlockDetour.usesPathRouter
    val usesHandRolledPolylineRule: Boolean = PathBlockDetour.usesHandRolledPolylineRule
    val prefersLiveValhalla: Boolean = PathBlockDetour.prefersLiveValhalla

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.PATH_BLOCK_DETOUR)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.PATH_BLOCK_DETOUR, enabled)
    }
}
