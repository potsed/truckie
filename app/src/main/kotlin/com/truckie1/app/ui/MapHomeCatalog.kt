package com.truckie1.app.ui

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

    const val composeOnly: Boolean = true
    const val allowsXmlLayouts: Boolean = false
    const val hostActivitySimpleName: String = "MainActivity"
    const val hostedInNavHost: Boolean = true
    const val keepsHighContrastChrome: Boolean = true
    const val mapEngineName: String = "MapLibre"

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
}
