package com.truckie1.app.ui

import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager

/**
 * One shared SymbolManager obtain / default-config path for home-map annotations
 * (route stops + bridge W5-8). Both feature catalogs rebind through
 * [MapLibreStyleLifecycle] orphan checks — not a parallel marker stack.
 *
 * Gherkin: features/map_stop_markers.feature, features/bridge_markers.feature.
 */
object MapSymbolBind {
    /** Catalog: stops and bridges share this obtain + defaults pipeline. */
    const val sharesPathWithBridgeAndStopMarkers: Boolean = true

    /** Style load / recreate rules live in [MapLibreStyleLifecycle]. */
    const val usesMapLibreStyleLifecycle: Boolean = true

    const val iconAllowOverlap: Boolean = true
    const val iconIgnorePlacement: Boolean = true
    const val textAllowOverlap: Boolean = true
    val textOptional: Boolean = MapLibreSymbolText.textOptional

    /**
     * Shared SymbolManager [iconSize] policy for home-map markers.
     * Baseline before ATNA-81 dogfood: stops 1.1, W5-8 1.15.
     * Daniel: stops **1.5×** (→ 1.65), W5-8 **0.5×** (→ 0.575).
     */
    object IconSize {
        /** Pre-ATNA-81 stop scale (shared baseline for the 1.5× bump). */
        const val STOP_BASELINE: Float = 1.1f

        /** Pre-ATNA-81 W5-8 scale (shared baseline for the 0.5× shrink). */
        const val BRIDGE_W58_BASELINE: Float = 1.15f

        /** Pickup / drop-off stop circles (~64px intrinsic) — 1.5× baseline. */
        const val STOP: Float = STOP_BASELINE * 1.5f

        /** AU W5-8 low-bridge diamond (~96px intrinsic) — 0.5× baseline. */
        const val BRIDGE_W58: Float = BRIDGE_W58_BASELINE * 0.5f

        /** Guardrail for dogfood / tests — neighbourhood zoom readability. */
        val neighbourhoodReadableRange: ClosedFloatingPointRange<Float> = 0.5f..2.0f

        fun isNeighbourhoodReadable(size: Float): Boolean =
            size in neighbourhoodReadableRange
    }

    /**
     * Shared SymbolManager defaults for OpenFreeMap Liberty (icons survive glyph
     * 404s via [textOptional]; overlap allowed so markers stay visible).
     */
    fun applySharedDefaults(manager: SymbolManager) {
        manager.iconAllowOverlap = iconAllowOverlap
        manager.iconIgnorePlacement = iconIgnorePlacement
        manager.textAllowOverlap = textAllowOverlap
        manager.textOptional = textOptional
    }

    /**
     * Drop the manager when its layer is missing from the live style, then
     * return an existing or newly created [SymbolManager] with shared defaults.
     */
    fun obtain(
        current: SymbolManager?,
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        clear: () -> Unit,
        assign: (SymbolManager) -> Unit,
        configure: (SymbolManager) -> Unit = {},
    ): SymbolManager {
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
        return live ?: SymbolManager(mapView, map, style).also { created ->
            applySharedDefaults(created)
            configure(created)
            assign(created)
        }
    }
}
