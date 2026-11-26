package com.truckie1.app.ui

import android.Manifest
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/** Anchors for the route/stop bottom drag sheet (peek / half / full). */
enum class RouteSheetValue {
    Peek,
    Half,
    Full,
}

/**
 * Catalog for map-first home (`ff_map_home`).
 * Gherkin: features/map_first_home.feature
 */
object MapHomeCatalog {
    val sheetAnchors: List<RouteSheetValue> = listOf(
        RouteSheetValue.Peek,
        RouteSheetValue.Half,
        RouteSheetValue.Full,
    )

    const val peekFraction: Float = 0.12f
    const val halfFraction: Float = 0.5f
    const val fullFraction: Float = 0.92f

    /** Sheet height changes via drag (AnchoredDraggable), not click-only. */
    const val usesDragGestures: Boolean = true
    const val usesMaterial3DragHandle: Boolean = true
    const val usesNestedScrollBridge: Boolean = true

    /**
     * Sheet Surface height equals the on-screen pull-up area (container − top offset),
     * not a full-screen Surface shoved down with an off-screen scroll viewport.
     * Gherkin: features/pull_up_drawer_scroll.feature
     */
    const val sizesSheetToVisibleHeight: Boolean = true

    /** Stops / create-route body scrolls inside the visible sheet (weight + verticalScroll). */
    const val stopsListScrollsWithinVisibleSheet: Boolean = true

    /** Nav-bar / home-indicator padding applies to visible sheet content. */
    const val appliesNavigationBarInsetsToSheetContent: Boolean = true

    /** IME padding so address fields stay reachable when the keyboard opens. */
    const val appliesImeInsetsToSheetContent: Boolean = true

    /**
     * Floating Jump-to-sample-bridges FAB is removed from map chrome.
     * Demo mode stays in Settings; camera helpers may remain for tests.
     */
    const val showsJumpToSampleBridgesFab: Boolean = false

    const val composeOnly: Boolean = true
    const val allowsXmlLayouts: Boolean = false
    const val hostActivitySimpleName: String = "MainActivity"
    const val hostedInNavHost: Boolean = true
    /** Sheet / overlays stay high-contrast; global TopAppBar+bottom bar are off on map home. */
    const val keepsHighContrastChrome: Boolean = true
    const val hidesGlobalHeaderFooter: Boolean = true
    const val mapEngineName: String = "MapLibre"

    /** Camera prefers the driver's current location; Brisbane is fallback only. */
    const val usesCurrentLocationCamera: Boolean = true
    const val fallsBackToBrisbaneWhenLocationUnavailable: Boolean = true
    /** Official stack: Play Services Fused Location Provider (not LocationManager-only). */
    const val usesFusedLocationProvider: Boolean = true
    const val usesPlayServicesLocationSettingsResolution: Boolean = true
    /** Blue my-location puck is gated separately by [FlagKeys.MAP_LOCATION_PUCK]. */
    const val supportsLocationPuck: Boolean = true
    /**
     * Follow mode (puck locked at centre, camera tracks) + Recenter when panned away.
     * Gated by [FlagKeys.MAP_HOME] + [FlagKeys.MAP_LOCATION_PUCK] via [MapLocationFollow].
     */
    const val supportsLocationFollow: Boolean = true
    const val supportsRecenterControl: Boolean = true

    val locationPermissions: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    val stopKindLabels: List<String> = listOf("pickup/load", "drop-off/unload")

    fun usesMapHome(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.MAP_HOME)

    fun showsSummaryStrip(value: RouteSheetValue): Boolean = true

    fun showsCreateRouteForm(value: RouteSheetValue): Boolean =
        value == RouteSheetValue.Half || value == RouteSheetValue.Full

    fun showsStopList(value: RouteSheetValue): Boolean =
        value == RouteSheetValue.Half || value == RouteSheetValue.Full

    fun fractionFor(value: RouteSheetValue): Float = when (value) {
        RouteSheetValue.Peek -> peekFraction
        RouteSheetValue.Half -> halfFraction
        RouteSheetValue.Full -> fullFraction
    }

    /**
     * Y offsets for [AnchoredDraggable] anchors (top of sheet from container top).
     * Peek is lowest sheet (largest Y); Full is highest sheet (smallest Y).
     */
    fun sheetAnchorOffsets(containerHeightPx: Float): Map<RouteSheetValue, Float> {
        val height = containerHeightPx.coerceAtLeast(1f)
        return sheetAnchors.associateWith { value ->
            height * (1f - fractionFor(value))
        }
    }

    /**
     * On-screen sheet height for a given drag offset (top of sheet from container top).
     * Keeps scroll / inset math inside the visible drawer — never below the fold.
     */
    fun visibleSheetHeightPx(containerHeightPx: Float, sheetTopOffsetPx: Float): Float {
        val height = containerHeightPx.coerceAtLeast(0f)
        val offset = sheetTopOffsetPx.coerceIn(0f, height)
        return (height - offset).coerceIn(0f, height)
    }
}

/**
 * Production polish tokens for the map-home route/stop drag sheet.
 * Gherkin: features/route_sheet_polish.feature — ships with [FlagKeys.MAP_HOME].
 */
object RouteSheetPolishCatalog {
    /** Larger top radius so the sheet reads as a Material3 sheet over the map. */
    const val topCornerRadiusDp: Int = 28

    /** Wider/taller than the default handle so it stays visible on busy map tiles. */
    const val dragHandleWidthDp: Int = 48
    const val dragHandleHeightDp: Int = 5
    const val dragHandleUsesOnSurfaceVariant: Boolean = true

    const val tonalElevationDp: Int = 3
    const val shadowElevationDp: Int = 8
    const val contentHorizontalPaddingDp: Int = 20
    const val sectionSpacingDp: Int = 20
    const val peekContentVerticalPaddingDp: Int = 4

    const val summaryUsesTitleSmall: Boolean = true
    const val createRouteUsesHeadlineTitle: Boolean = true
    const val dateChoicesUseOutlinedCards: Boolean = true
    const val addFindUsesSearchLeadingIcon: Boolean = true
    const val stopKindsUseFilterChips: Boolean = true
    const val suggestionsUseSurfaceRows: Boolean = true
    const val stopRowsUseTimelineBadge: Boolean = true

    const val preservesDragGestures: Boolean = true
    const val hidesGlobalHeaderFooter: Boolean = true
    const val domainUsesRouteAndStops: Boolean = true
    val stopKindLabels: List<String> = MapHomeCatalog.stopKindLabels
}
