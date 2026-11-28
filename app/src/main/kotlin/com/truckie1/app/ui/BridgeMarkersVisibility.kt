package com.truckie1.app.ui

import com.truckie1.maps.MapCameraDefaults
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Home-map visibility contract for `ff_bridge_markers`.
 * Sample pack must be findable at street-neighbourhood zoom around Brisbane fallback.
 */
object BridgeMarkersVisibility {
    /** At ZOOM 15.5, keep several markers inside ~2 km of CBD. */
    const val SEQ_RADIUS_KM: Double = 2.0
    const val MIN_SEQ_BRIDGES: Int = 3

    /** MapLibreHost loads OpenFreeMap style once; recompositions only rebind overlays. */
    const val loadsStyleOnce: Boolean = true

    /** Concurrent AndroidView updates must enqueue, not start a second setStyle. */
    const val coalescesConcurrentStyleLoads: Boolean = true

    /** When style is (re)created, GeoJSON overlay layers are rebuilt for that style. */
    const val recreatesOverlaysOnNewStyle: Boolean = true

    /** If map.style !== the Style overlays were bound to, drop and recreate them. */
    const val recreatesOverlaysWhenStyleIdentityDrifts: Boolean = true

    /** If an overlay layer id is missing from the live style, recreate that overlay. */
    const val recreatesOverlaysWhenLayerMissing: Boolean = true

    /**
     * Annotation-era names — always false after ATNA-117 (style layers only).
     * Prefer [recreatesOverlaysOnNewStyle] / drift / layer-missing flags.
     */
    const val recreatesManagersOnNewStyle: Boolean = false
    const val recreatesManagersWhenStyleIdentityDrifts: Boolean = false
    const val recreatesManagersWhenLayerMissing: Boolean = false

    /**
     * Relative symbol-layer scale for the W5-8 PNG (~96px intrinsic).
     * Shared with stop markers via [MapStyleOverlayCatalog.IconSize.BRIDGE_W58].
     */
    val iconSize: Float = MapStyleOverlayCatalog.IconSize.BRIDGE_W58

    const val usesAuRoadSignW58: Boolean = true

    /**
     * Clearance labels must use OpenFreeMap’s Noto Sans stack — MapLibre’s default
     * Open Sans fontstack 404s and hides the whole W5-8 symbol (icon + text).
     */
    const val requiresOpenFreeMapCompatibleTextFont: Boolean =
        MapLibreSymbolText.requiresOpenFreeMapCompatibleTextFont

    val symbolTextFont: Array<String> = MapLibreSymbolText.openFreeMapTextFont

    fun nearBrisbaneFallback(bridges: List<BridgePoint>): List<BridgePoint> =
        bridges.filter { bridge ->
            distanceKm(
                MapCameraDefaults.FALLBACK_LATITUDE,
                MapCameraDefaults.FALLBACK_LONGITUDE,
                bridge.latitude,
                bridge.longitude,
            ) <= SEQ_RADIUS_KM
        }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthKm * c
    }
}
