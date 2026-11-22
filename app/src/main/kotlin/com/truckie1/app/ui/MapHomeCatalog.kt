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
