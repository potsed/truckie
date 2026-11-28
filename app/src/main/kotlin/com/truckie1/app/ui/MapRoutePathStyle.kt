package com.truckie1.app.ui

/**
 * MapLibre line style for in-app PathRouter paths (Navigate + home route
 * polyline). Shared [MapStyleOverlayBind] GeoJSON LineString + line layer —
 * not Annotation LineManager (ATNA-117).
 */
object MapRoutePathStyle {
    /** Truckie navy — high contrast on OpenFreeMap Liberty. */
    const val LINE_COLOR_HEX: String = "#0B1F33"
    const val LINE_WIDTH: Float = 5.0f
    const val usesStyleLineLayer: Boolean = true
    const val usesLineManager: Boolean = false
    const val usesSharedStyleOverlayBindPath: Boolean = true
}

/** @deprecated Use [MapRoutePathStyle]; kept for older call sites during ATNA-117. */
@Deprecated(
    message = "Use MapRoutePathStyle (style line layer, not Annotation LineManager)",
    replaceWith = ReplaceWith("MapRoutePathStyle"),
)
typealias MapRoutePathAnnotations = MapRoutePathStyle
