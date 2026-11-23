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
     * Queensland Rail SEQ road-under-rail clearances (ATNA-44): show published
     * QR railbridges pack entries on the map; off keeps BCC CBD samples only.
     */
    const val QUEENSLAND_RAIL_CLEARANCES = "ff_queensland_rail_clearances"
    /**
     * W5-8 only for low / restricted clearances (ATNA-45): AU W5-8 diamond
     * only when verified low (≤ 4.6 m) or QR published underbridge.
     */
    const val W5_8_LOW_ONLY = "ff_w5_8_low_only"
    /**
     * TMR Structure Clearances in Queensland (ATNA-46): show published TMR
     * spatial clearance pack entries; off hides TMR rows only.
     */
    const val TMR_STRUCTURE_CLEARANCES = "ff_tmr_structure_clearances"
    /**
     * BCC Road Height Restrictions GIS (ATNA-46): show Brisbane Open Data
     * under-4.6 m local pack entries; off keeps curated CBD `-sample` rows.
     */
    const val BCC_ROAD_HEIGHT_RESTRICTIONS = "ff_bcc_road_height_restrictions"
    /**
     * Demo mode — temporary dogfood affordances (e.g. Jump to sample bridges).
     * May later gate a fuller demo dataset; that is out of scope for the jump control.
     */
    const val DEMO_MODE = "ff_demo_mode"
    /**
     * Home-map W5-8 symbols filtered by active vehicle height + system buffer
     * (ATNA-47): show only structures the vehicle cannot clear. When no usable
     * vehicle height, falls back to all low-pack markers.
     */
    const val VEHICLE_HEIGHT_BRIDGE_FILTER = "ff_vehicle_height_bridge_filter"
    /**
     * Bridge passage over vs under (ATNA-48): model under/over/both/unknown.
     * Height conflicts block driving under a restricted structure — OK to drive
     * over the deck. Never invents clearances.
     */
    const val BRIDGE_PASSAGE = "ff_bridge_passage"
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
