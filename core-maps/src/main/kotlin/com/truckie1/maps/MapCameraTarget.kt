package com.truckie1.maps

/**
 * Resolved MapLibre camera centre. Prefers the driver's current location;
 * falls back to [MapCameraDefaults] Brisbane when a fix is unavailable.
 */
data class MapCameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double = MapCameraDefaults.ZOOM,
    val usedFallback: Boolean,
) {
    companion object {
        fun resolve(
            latitude: Double?,
            longitude: Double?,
            zoom: Double = MapCameraDefaults.ZOOM,
        ): MapCameraTarget {
            if (latitude != null && longitude != null) {
                return MapCameraTarget(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = zoom,
                    usedFallback = false,
                )
            }
            return MapCameraTarget(
                latitude = MapCameraDefaults.FALLBACK_LATITUDE,
                longitude = MapCameraDefaults.FALLBACK_LONGITUDE,
                zoom = zoom,
                usedFallback = true,
            )
        }

        fun fallback(zoom: Double = MapCameraDefaults.ZOOM): MapCameraTarget =
            resolve(latitude = null, longitude = null, zoom = zoom)
    }
}
