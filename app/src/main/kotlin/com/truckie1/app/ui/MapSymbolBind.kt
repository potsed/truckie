package com.truckie1.app.ui

/**
 * Legacy name for shared home-map overlay icon / bind contracts.
 *
 * ATNA-117 deleted Annotation SymbolManager obtain — overlays bind through
 * [MapStyleOverlayBind]. Kept as a thin facade so older tests / docs that
 * named [MapSymbolBind.IconSize] still resolve.
 *
 * Gherkin: features/map_style_overlays.feature
 */
object MapSymbolBind {
    const val sharesPathWithBridgeAndStopMarkers: Boolean =
        MapStyleOverlayCatalog.sharesOneBindPathForBridgesStopsAndRoute

    const val usesMapLibreStyleLifecycle: Boolean =
        MapStyleOverlayCatalog.usesMapLibreStyleLifecycle

    const val iconAllowOverlap: Boolean = MapStyleOverlayCatalog.iconAllowOverlap
    const val iconIgnorePlacement: Boolean = MapStyleOverlayCatalog.iconIgnorePlacement
    const val textAllowOverlap: Boolean = MapStyleOverlayCatalog.textAllowOverlap
    val textOptional: Boolean = MapStyleOverlayCatalog.textOptional

    /** @see MapStyleOverlayCatalog.IconSize */
    object IconSize {
        const val STOP_BASELINE: Float = MapStyleOverlayCatalog.IconSize.STOP_BASELINE
        const val BRIDGE_W58_BASELINE: Float = MapStyleOverlayCatalog.IconSize.BRIDGE_W58_BASELINE
        const val STOP: Float = MapStyleOverlayCatalog.IconSize.STOP
        const val BRIDGE_W58: Float = MapStyleOverlayCatalog.IconSize.BRIDGE_W58
        val neighbourhoodReadableRange: ClosedFloatingPointRange<Float> =
            MapStyleOverlayCatalog.IconSize.neighbourhoodReadableRange

        fun isNeighbourhoodReadable(size: Float): Boolean =
            MapStyleOverlayCatalog.IconSize.isNeighbourhoodReadable(size)
    }
}
