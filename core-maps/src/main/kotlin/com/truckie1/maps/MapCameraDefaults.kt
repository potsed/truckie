package com.truckie1.maps

/**
 * Default MapLibre camera for Truckie Phase A (AU / Brisbane metro).
 * Offline packs land later behind [com.truckie1.flags.FlagKeys.OFFLINE_MAPS].
 */
object MapCameraDefaults {
    /** Brisbane CBD approximate centre. */
    const val LATITUDE: Double = -27.4698

    const val LONGITUDE: Double = 153.0251

    /** Metro overview — enough to see Brisbane / SE QLD context. */
    const val ZOOM: Double = 11.0

    /**
     * Public MapLibre demotiles style (no API key). Replace with a production
     * style / self-hosted tiles when offline packs and branding land.
     */
    const val STYLE_URI: String = "https://demotiles.maplibre.org/style.json"
}
