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

    /** MapLibreHost loads OpenFreeMap style once; recompositions only rebind annotations. */
    const val loadsStyleOnce: Boolean = true

    /** When style is (re)created, SymbolManagers / LineManager are rebuilt for that style. */
    const val recreatesManagersOnNewStyle: Boolean = true

    /**
     * Relative SymbolManager scale for the W5-8 PNG (~96px intrinsic).
     * ~1.15 reads as a sensible road-sign size at street-neighbourhood zoom.
     */
    const val iconSize: Float = 1.15f

    const val usesAuRoadSignW58: Boolean = true

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
