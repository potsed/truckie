package com.truckie1.app.ui

/**
 * Shared MapLibre **style-layer** overlay contracts for bridges, stops, and
 * route polyline (ATNA-117).
 *
 * Replaces Annotation [org.maplibre.android.plugins.annotation.SymbolManager] /
 * LineManager with [org.maplibre.android.style.sources.GeoJsonSource] +
 * symbol / line layers. Visual constants match the Annotation-era values
 * (icon sizes, label fonts/colours/offsets, line colour/width).
 *
 * Gherkin: features/map_style_overlays.feature
 */
object MapStyleOverlayIds {
    const val BRIDGE_SOURCE: String = "truckie-bridges-source"
    const val BRIDGE_LAYER: String = "truckie-bridges-symbol"
    const val STOP_SOURCE: String = "truckie-stops-source"
    const val STOP_LAYER: String = "truckie-stops-symbol"
    const val ROUTE_SOURCE: String = "truckie-route-source"
    const val ROUTE_LAYER: String = "truckie-route-line"
}

object MapStyleOverlayCatalog {
    const val usesGeoJsonSource: Boolean = true
    const val usesStyleSymbolLayers: Boolean = true
    const val usesStyleLineLayer: Boolean = true
    const val usesAnnotationSymbolManager: Boolean = false
    const val usesAnnotationLineManager: Boolean = false
    const val usesDeprecatedMarkerApi: Boolean = false
    const val sharesOneBindPathForBridgesStopsAndRoute: Boolean = true
    const val usesMapLibreStyleLifecycle: Boolean = true
    /** Recenter / follow stay on LocationComponent — overlays must not touch them. */
    const val preservesRecenterAndFollow: Boolean = true

    const val iconAllowOverlap: Boolean = true
    const val iconIgnorePlacement: Boolean = true
    const val textAllowOverlap: Boolean = true
    val textOptional: Boolean = MapLibreSymbolText.textOptional

    /**
     * Shared iconSize policy for home-map markers (Annotation-era Daniel scales).
     * Baseline before ATNA-81 dogfood: stops 1.1, W5-8 1.15.
     * Daniel: stops **1.5×** (→ 1.65), W5-8 **0.5×** (→ 0.575).
     */
    object IconSize {
        const val STOP_BASELINE: Float = 1.1f
        const val BRIDGE_W58_BASELINE: Float = 1.15f
        const val STOP: Float = STOP_BASELINE * 1.5f
        const val BRIDGE_W58: Float = BRIDGE_W58_BASELINE * 0.5f
        val neighbourhoodReadableRange: ClosedFloatingPointRange<Float> = 0.5f..2.0f

        fun isNeighbourhoodReadable(size: Float): Boolean =
            size in neighbourhoodReadableRange
    }

    /** Clearance label paint — matches prior SymbolOptions on W5-8. */
    object BridgeLabel {
        const val TEXT_SIZE: Float = 12f
        const val TEXT_COLOR_HEX: String = "#0B1F33"
        const val TEXT_HALO_COLOR_HEX: String = "#FFFFFF"
        const val TEXT_HALO_WIDTH: Float = 1.4f
        val TEXT_OFFSET: FloatArray = floatArrayOf(0f, -1.6f)
        const val TEXT_ANCHOR: String = "bottom"
        const val ICON_ANCHOR: String = "center"
    }

    /** Stop index label paint — matches prior SymbolOptions on stop circles. */
    object StopLabel {
        const val TEXT_SIZE: Float = 12f
        const val TEXT_COLOR_HEX: String = "#FFFFFF"
        const val TEXT_HALO_COLOR_HEX: String = "#0B1F33"
        const val TEXT_HALO_WIDTH: Float = 1.0f
        val TEXT_OFFSET: FloatArray = floatArrayOf(0f, 0.05f)
        const val ICON_ANCHOR: String = "center"
    }
}
