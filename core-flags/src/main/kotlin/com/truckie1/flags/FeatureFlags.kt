package com.truckie1.flags

/**
 * Feature-flag facade. Every user-facing capability must be gated.
 */
interface FeatureFlags {
    fun isEnabled(key: String): Boolean
}

object FlagKeys {
    const val VEHICLE_PROFILES = "ff_vehicle_profiles"
    const val MULTI_STOP = "ff_multi_stop"
    const val STOP_CARGO = "ff_stop_cargo"
    const val BRIDGE_SAFE_ROUTING = "ff_bridge_safe_routing"
    /** Fastest / Cheapest among height- and weight-safe tours. */
    const val ROUTE_OPTIMIZE = "ff_route_optimize"

    @Deprecated("Use ROUTE_OPTIMIZE (ff_route_optimize)", ReplaceWith("ROUTE_OPTIMIZE"))
    const val OPTIMIZE_FOR_VEHICLE = ROUTE_OPTIMIZE
    const val TOLLS = "ff_tolls"
    const val OFFLINE_MAPS = "ff_offline_maps"
    const val ANDROID_AUTO = "ff_android_auto"
    const val NHVR_OVERLAY = "ff_nhvr_overlay"
    /** Material3 TopAppBar + bottom NavigationBar across primary screens. */
    const val NAV_CHROME = "ff_nav_chrome"
    /** MapLibre map-first home + route/stop drag sheet (Phase A). */
    const val MAP_HOME = "ff_map_home"
    /** Verified address autocomplete (Nominatim AU default) for route stops (Phase B). */
    const val ADDRESS_AUTOCOMPLETE = "ff_address_autocomplete"
    /** Blue my-location puck on the MapLibre map (updates with Fused Location). */
    const val MAP_LOCATION_PUCK = "ff_map_location_puck"
    /** Create route requires verified start + end (same place = round trip). */
    const val ROUTE_START_END = "ff_route_start_end"
    /**
     * Home Navigate: GraphHopper path drawn on the same MapLibre map
     * (stub path until live GraphHopper HTTP). No external maps intents.
     */
    const val NAVIGATE = "ff_navigate"
    /** Low-bridge markers on MapLibre home (Australia-first sample/fixture asset). */
    const val BRIDGE_MARKERS = "ff_bridge_markers"
    /**
     * Verified CBD sample clearances (ATNA-42): labels only for published road
     * heights; unknown / not_applicable omit invented mocks.
     */
    const val VERIFIED_CBD_CLEARANCES = "ff_verified_cbd_clearances"
    /**
     * Demo mode — temporary dogfood affordances (e.g. Jump to sample bridges).
     * May later gate a fuller demo dataset; that is out of scope for the jump control.
     */
    const val DEMO_MODE = "ff_demo_mode"
}

/**
 * Local in-memory flags. Production defaults are off.
 */
class LocalFeatureFlags(
    initial: Map<String, Boolean> = emptyMap(),
) : FeatureFlags {
    private val overrides = initial.toMutableMap()

    override fun isEnabled(key: String): Boolean = overrides[key] == true

    fun setEnabled(key: String, enabled: Boolean) {
        overrides[key] = enabled
    }
}
