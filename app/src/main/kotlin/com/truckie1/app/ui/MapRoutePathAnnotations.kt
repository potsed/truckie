package com.truckie1.app.ui

/**
 * MapLibre line style for in-app PathRouter paths (Navigate + home route
 * polyline). Both features share [MapLineBind] / LineManager — not a parallel
 * line stack.
 */
object MapRoutePathAnnotations {
    /** Truckie navy — high contrast on OpenFreeMap Liberty. */
    const val LINE_COLOR_HEX: String = "#0B1F33"
    const val LINE_WIDTH: Float = 5.0f
    const val usesLineManager: Boolean = true
    const val usesSharedLineBindPath: Boolean = true
}
