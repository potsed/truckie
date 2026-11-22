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
     * OpenFreeMap Liberty — free OSM street basemap for MapLibre (no API key).
     * Demotiles only shows continent polygons (no roads); do not use for dogfood.
     * Swap to a production / self-hosted style when offline packs land.
     */
    const val STYLE_URI: String = "https://tiles.openfreemap.org/styles/liberty"

    /** Human-readable source label for docs / catalog tests. */
    const val STYLE_SOURCE_LABEL: String = "OpenFreeMap Liberty (OSM vector tiles)"

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
