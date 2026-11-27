package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Navigate multi-stop tour path (`ff_navigate_multi_stop`, ATNA-87 / P1-2).
 *
 * Navigate uses [com.truckie1.app.routing.ActiveTourPath] +
 * [com.truckie1.routing.MultiStopTourComposer] ordered PathRouter legs — the
 * same geometry as Optimise + map polyline. The legacy start→end-only
 * Navigate path is deleted.
 *
 * Defaults **on**. Requires [FlagKeys.NAVIGATE] for the Navigate action.
 *
 * Gherkin: features/navigate_multi_stop_path.feature
 */
object NavigateMultiStopCatalog {
    const val FLAG_KEY: String = FlagKeys.NAVIGATE_MULTI_STOP

    /** Ship / dogfood default after ATNA-87. */
    const val DEFAULT_ENABLED: Boolean = true

    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    const val usesActiveTourPath: Boolean = true
    const val usesMultiStopTourComposer: Boolean = true
    const val allowsStartToEndOnlyPath: Boolean = false
    const val sharesGeometryWithMapPolyline: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NAVIGATE_MULTI_STOP)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.NAVIGATE_MULTI_STOP, enabled)
    }
}
