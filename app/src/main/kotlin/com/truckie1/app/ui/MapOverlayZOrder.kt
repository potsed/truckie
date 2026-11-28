package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Layer

/**
 * MapLibre native overlay stack order (ATNA-122).
 *
 * Bottom → top among Truckie style overlays: bridges → route line → stops.
 * Placement uses [Style.addLayerAbove] / [Style.addLayerBelow] — not Annotation
 * zIndex.
 *
 * Gherkin: features/map_overlay_z_order.feature
 */
object MapOverlayZOrderCatalog {
    const val FLAG_KEY: String = FlagKeys.MAP_OVERLAY_Z_ORDER

    /** Defaults on — readable stop pins over the polyline is select-user SoT. */
    const val DEFAULT_ENABLED: Boolean = true
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    const val usesNativeAddLayerAbove: Boolean = true
    const val usesAnnotationZIndex: Boolean = false

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_OVERLAY_Z_ORDER)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.MAP_OVERLAY_Z_ORDER, enabled)
    }
}

/**
 * Pure policy + Style helpers for Truckie overlay z-order.
 *
 * [bottomToTop] is the sole contract: stop points top, route next, others below.
 */
object MapOverlayZOrder {
    /** Bottom → top among Truckie overlay layer ids. */
    val bottomToTop: List<String> = listOf(
        MapStyleOverlayIds.BRIDGE_LAYER,
        MapStyleOverlayIds.ROUTE_LAYER,
        MapStyleOverlayIds.STOP_LAYER,
    )

    val bottomOverlayLayerId: String = bottomToTop.first()
    val topOverlayLayerId: String = bottomToTop.last()

    sealed class Placement {
        data object Top : Placement()
        data class Above(val layerId: String) : Placement()
        data class Below(val layerId: String) : Placement()
    }

    /** Overlay ids from [layersBottomToTop] preserving relative stack order. */
    fun overlayIdsInStackOrder(layersBottomToTop: List<String>): List<String> {
        val wanted = bottomToTop.toSet()
        return layersBottomToTop.filter { it in wanted }
    }

    /**
     * True when present Truckie overlays appear in [bottomToTop] order
     * (missing siblings are ignored).
     */
    fun isCorrectRelativeOrder(layersBottomToTop: List<String>): Boolean {
        val present = overlayIdsInStackOrder(layersBottomToTop)
        if (present.size <= 1) return true
        var lastIndex = -1
        for (id in present) {
            val idx = bottomToTop.indexOf(id)
            if (idx < lastIndex) return false
            lastIndex = idx
        }
        return true
    }

    fun needsReconcile(layersBottomToTop: List<String>): Boolean =
        !isCorrectRelativeOrder(layersBottomToTop)

    /**
     * Where to insert [layerId] given siblings already on the style.
     * Prefers [Placement.Above] the nearest lower sibling, else
     * [Placement.Below] the nearest higher sibling, else [Placement.Top].
     */
    fun placementFor(layerId: String, presentIds: Set<String>): Placement {
        val index = bottomToTop.indexOf(layerId)
        require(index >= 0) { "Unknown overlay layer id: $layerId" }
        val below = bottomToTop.take(index).lastOrNull { it in presentIds }
        if (below != null) return Placement.Above(below)
        val above = bottomToTop.drop(index + 1).firstOrNull { it in presentIds }
        if (above != null) return Placement.Below(above)
        return Placement.Top
    }

    /** Apply [placement] via MapLibre native Style layer APIs. */
    fun addLayer(style: Style, layer: Layer, placement: Placement) {
        when (placement) {
            is Placement.Top -> style.addLayer(layer)
            is Placement.Above -> style.addLayerAbove(layer, placement.layerId)
            is Placement.Below -> style.addLayerBelow(layer, placement.layerId)
        }
    }

    /**
     * Re-stack present Truckie overlays bottom→top with [Style.addLayerAbove]
     * so stops sit above the route line and bridges stay below both.
     */
    fun reconcile(style: Style) {
        val present = bottomToTop.mapNotNull { id ->
            style.getLayer(id)?.let { id to it }
        }
        if (present.isEmpty()) return
        // Always restack at top so LocationComponent / label layers inserted
        // mid-bind stay below Truckie overlays, with stops > route > bridges.
        restackAtTop(style, present)
    }

    private fun restackAtTop(style: Style, present: List<Pair<String, Layer>>) {
        present.forEach { (_, layer) -> style.removeLayer(layer) }
        present.forEachIndexed { i, (_, layer) ->
            if (i == 0) {
                style.addLayer(layer)
            } else {
                style.addLayerAbove(layer, present[i - 1].first)
            }
        }
    }
}
