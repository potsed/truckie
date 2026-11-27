package com.truckie1.app.ui

import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager

/**
 * One shared LineManager obtain path for Navigate + home route polyline.
 * Both rebind through [MapLibreStyleLifecycle] orphan checks — not a parallel
 * line stack.
 *
 * Gherkin: features/map_route_polyline.feature.
 */
object MapLineBind {
    const val sharesPathWithNavigatePolyline: Boolean = true
    const val usesMapLibreStyleLifecycle: Boolean = true

    /**
     * Drop the manager when its layer is missing from the live style, then
     * return an existing or newly created [LineManager].
     */
    fun obtain(
        current: LineManager?,
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        clear: () -> Unit,
        assign: (LineManager) -> Unit,
    ): LineManager {
        var live = current
        if (live != null) {
            val recreate = MapLibreStyleLifecycle.mustRecreateAnnotationManager(
                hasManager = true,
                styleContainsManagerLayer = style.getLayer(live.layerId) != null,
            )
            if (recreate) {
                runCatching { live.onDestroy() }
                clear()
                live = null
            }
        }
        return live ?: LineManager(mapView, map, style).also { created ->
            assign(created)
        }
    }
}
