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

    /** Slightly larger than 1.0 so amber triangles read at neighbourhood zoom. */
    const val iconSize: Float = 1.35f

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
