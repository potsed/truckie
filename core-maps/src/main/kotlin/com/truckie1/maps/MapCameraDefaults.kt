package com.truckie1.maps

/**
 * MapLibre camera defaults for Truckie.
 * Preferred centre is the driver's **current location** (Play Services Fused Location).
 * Brisbane is the fallback when a fix is unavailable or permission is denied.
 * Offline packs land later behind [com.truckie1.flags.FlagKeys.OFFLINE_MAPS].
 */
object MapCameraDefaults {
    /** Brisbane CBD — used only when current location is unavailable. */
    const val FALLBACK_LATITUDE: Double = -27.4698

    const val FALLBACK_LONGITUDE: Double = 153.0251

    /** Metro overview zoom around the resolved centre. */
    const val ZOOM: Double = 11.0

    /**
     * Public MapLibre demotiles style (no API key). Replace with a production
     * style / self-hosted tiles when offline packs and branding land.
     */
    const val STYLE_URI: String = "https://demotiles.maplibre.org/style.json"

    @Deprecated(
        "Use FALLBACK_LATITUDE — camera prefers current location",
        ReplaceWith("FALLBACK_LATITUDE"),
    )
    const val LATITUDE: Double = FALLBACK_LATITUDE

    @Deprecated(
        "Use FALLBACK_LONGITUDE — camera prefers current location",
        ReplaceWith("FALLBACK_LONGITUDE"),
    )
    const val LONGITUDE: Double = FALLBACK_LONGITUDE
}
