package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Architecture catalog for map-home / RootNav / MapLibre god-file split
 * (ATNA-90 / P1-5).
 *
 * Product behaviour stays behind existing [FlagKeys.MAP_HOME] and related map
 * flags. This catalog documents the **only** orchestration shape after the
 * refactor — no dual inline MapHomeRoute domain path.
 *
 * Gherkin: features/map_home_orchestration.feature
 */
object MapHomeOrchestrationCatalog {
    /** Existing product gate — orchestration split is not a separate ship toggle. */
    const val PRODUCT_FLAG_KEY: String = FlagKeys.MAP_HOME

    /** Map-home domain mutations live in [MapHomeController], not RootNav. */
    const val usesMapHomeController: Boolean = true

    /** [MapHomeRoute] is a thin Compose binder over the controller. */
    const val mapHomeRouteIsThinBinder: Boolean = true

    /** No parallel inline god MapHomeRoute domain body in TruckieRootNav. */
    const val deletesInlineMapHomeRouteDomain: Boolean = true

    /** [MapLibreHost] is a dedicated type / file — command executor only. */
    const val mapLibreHostIsDedicatedType: Boolean = true

    /** Follow / puck / Recenter / heading still: reconcile → executeFollowCommands. */
    const val followUsesReconcileThenHost: Boolean = true

    /** Bind applies one [MapLibreBindSnapshot]; host does not re-query app flags. */
    const val hostBindsPrecomputedSnapshot: Boolean = true

    /** Host must not cast Context → TruckieApp for feature flags during bind. */
    const val hostDoesNotLookupAppFlags: Boolean = true

    /** Route sheet widgets live in dedicated modules (not only MapHomeScreen.kt). */
    const val routeSheetModulesExtracted: Boolean = true

    /** RootNav destination controllers live in dedicated files. */
    const val rootNavDestinationsSplit: Boolean = true

    /** Legacy Activities stay until ATNA-74 — compile touchpoints only. */
    const val deletesLegacyActivities: Boolean = false

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_HOME)
}
