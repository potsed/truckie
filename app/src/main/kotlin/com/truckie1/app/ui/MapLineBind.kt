package com.truckie1.app.ui

/**
 * Legacy name for shared home-map / Navigate route line bind.
 *
 * ATNA-117 deleted Annotation LineManager obtain — route lines bind through
 * [MapStyleOverlayBind]. Kept as a thin facade for older tests / catalog refs.
 *
 * Gherkin: features/map_style_overlays.feature, features/map_route_polyline.feature
 */
object MapLineBind {
    const val sharesPathWithNavigatePolyline: Boolean =
        MapRoutePathStyle.usesSharedStyleOverlayBindPath

    const val usesMapLibreStyleLifecycle: Boolean =
        MapStyleOverlayCatalog.usesMapLibreStyleLifecycle
}
