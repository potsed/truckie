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
    /**
     * Offline MapLibre pack manager (ATNA-95 contracts). Defaults off in
     * production **and** debug until OfflineManager + pack UX + offline style.
     * See features/offline_maps.feature.
     */
    const val OFFLINE_MAPS = "ff_offline_maps"
    /**
     * Android Auto surfaces (ATNA-10 / ATNA-95). Defaults off in production
     * **and** debug; requires API 23+. See features/android_auto.feature.
     */
    const val ANDROID_AUTO = "ff_android_auto"
    /**
     * NHVR / state restriction overlays (ATNA-11 / ATNA-43 / ATNA-95).
     * Defaults off in production **and** debug. See features/nhvr_overlay.feature.
     */
    const val NHVR_OVERLAY = "ff_nhvr_overlay"
    /** Material3 TopAppBar + bottom NavigationBar across primary screens. */
    const val NAV_CHROME = "ff_nav_chrome"
    /** MapLibre map-first home + route/stop drag sheet (Phase A). */
    const val MAP_HOME = "ff_map_home"
    /** Verified address autocomplete (Nominatim AU default) for route stops (Phase B). */
    const val ADDRESS_AUTOCOMPLETE = "ff_address_autocomplete"
    /**
     * G-NAF premise geocode (ATNA-108): when autocomplete is on, resolve typed
     * unit/housenumber to Geocoded National Address File points (local pack)
     * instead of Nominatim street centroids. Never invents coordinates.
     */
    const val GNAF_ADDRESS_GEOCODE = "ff_gnaf_address_geocode"
    /** Blue my-location puck on the MapLibre map (updates with Fused Location). */
    const val MAP_LOCATION_PUCK = "ff_map_location_puck"
    /** Create route requires verified start + end (same place = round trip). */
    const val ROUTE_START_END = "ff_route_start_end"
    /**
     * Home Navigate: PathRouter path drawn on the same MapLibre map
     * (stub until live Valhalla HTTP). No external maps intents.
     */
    const val NAVIGATE = "ff_navigate"
    /** Low-bridge markers on MapLibre home (Australia-first sample/fixture asset). */
    const val BRIDGE_MARKERS = "ff_bridge_markers"
    /**
     * Route stop markers on MapLibre home (ATNA-81): pickup vs drop-off
     * SymbolManager icons from the active AppDataSource route. Shares the
     * bridge SymbolManager / style lifecycle bind path.
     */
    const val STOP_MARKERS = "ff_stop_markers"
    /**
     * Active route polyline on MapLibre home (ATNA-82): PathRouter /
     * MultiStopTourComposer constrained geometry via shared LineManager
     * with Navigate. Stub geometry only when stub PathRouter is active.
     */
    const val MAP_ROUTE_POLYLINE = "ff_map_route_polyline"
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
     * Demo mode (ATNA-56 / ATNA-75) — Settings selects demo AppDataSource
     * (same schema, read-only sample). Jump FAB removed (ATNA-67).
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
    /**
     * Consume the public AU low-bridges JSON pack (ATNA-50) as Truckie's source
     * pack — local-first refresh from GitHub Release assets when newer.
     * Shared full schema with potsed/au-low-bridges (no thinner parallel).
     */
    const val AU_LOW_BRIDGES_PACK = "ff_au_low_bridges_pack"
    /**
     * While following, orient the map to the direction of travel (course-up /
     * heading-up via MapLibre TRACKING_GPS). Toggled from map chrome (like
     * Recenter), not Settings. Off = north-up TRACKING.
     */
    const val MAP_HEADING_UP = "ff_map_heading_up"
    /**
     * Prefer street / road name labels over POI clutter on OpenFreeMap Liberty
     * (ATNA-78). Runtime layer visibility + minzoom tweaks after style load;
     * keeps the same OSS style URL (no paid / rehosted tiles).
     */
    const val MAP_STREET_LABELS = "ff_map_street_labels"
    /**
     * Live Valhalla PathRouter (ATNA-113): hosted Valhalla interim (e.g. Simple
     * Routing) or self-hosted / offline tiles. When on **and** credentials
     * resolve a base URL, Navigate / Optimise use Valhalla truck costing;
     * otherwise stub (`path-stub`). Sole live routing engine.
     */
    const val VALHALLA_LIVE = "ff_valhalla_live"
    /**
     * While navigating, always show the next destination address (upcoming stop)
     * on map-home (ATNA-83) — not turn-only chrome. Drivers may know a better way.
     * Does not touch LineManager / MapRoutePathAnnotations.
     */
    const val NEXT_DESTINATION_DISPLAY = "ff_next_destination_display"
    /**
     * Single active-tour facade (ATNA-86 / P1-1): map-home Route + Plan Trip /
     * Optimise + demo share one [com.truckie1.trips.ActiveTourSession].
     * TripStop is a projection; Route remains the editable SoT.
     */
    const val ACTIVE_TOUR = "ff_active_tour"
    /**
     * Navigate multi-stop tour path (ATNA-87 / P1-2): Navigate consumes the same
     * ordered MultiStopTourComposer / PathRouter geometry as Optimise + map
     * polyline via ActiveTourPath. Start→end-only Navigate path is deleted.
     * Defaults on.
     */
    const val NAVIGATE_MULTI_STOP = "ff_navigate_multi_stop"
    /**
     * Route path approach from current location (ATNA-99): ActiveTourPath
     * prepends a PathRouter leg from the fused fix to route start, then
     * ordered stops → end. When no fix, falls back to start → stops → end.
     * Defaults on.
     */
    const val ROUTE_PATH_FROM_LOCATION = "ff_route_path_from_location"
    /**
     * Async Valhalla + Nominatim I/O (ATNA-88 / P1-3): live PathRouter and
     * Nominatim are suspend and hop off the Compose main thread. Sync-on-main
     * live HTTP is deleted. Defaults on. Does not enable [VALHALLA_LIVE].
     */
    const val ASYNC_ROUTING_IO = "ff_async_routing_io"
    /**
     * Room-persist active route + stops (ATNA-89 / P1-4): Real AppDataSource
     * ActiveTour reads/writes through Room. Demo stays isolated read-only
     * in-memory. Defaults on. No Firebase.
     */
    const val ROOM_ROUTE_PERSIST = "ff_room_route_persist"
    /**
     * Alternate around geographic path blocks via PathRouter (ATNA-102):
     * live Valhalla truck costing / exclude polygons — never erase the tour
     * polyline. Stub PathRouter only for tests / when live credentials unset.
     * Defaults on.
     */
    const val PATH_BLOCK_DETOUR = "ff_path_block_detour"
    /**
     * Owned AU address pack (ATNA-109 / ATNA-111): versioned local-first
     * **G-NAF-only** JSON pack as stop-geocode SoT (QLD Kallangur bootstrap →
     * downloadable QLD; one pack/search path; no multi-dataset merges).
     * Nominatim = typeahead UI at most until G-NAF resolves the hit.
     */
    const val OWNED_ADDRESS_PACK = "ff_owned_address_pack"
    /**
     * Settings safety disclaimer (ATNA-96): not a permit; roadside signs
     * override packs; NHVR / authority verify; weight is guidance only.
     * Defaults on for Play / select-user.
     */
    const val SETTINGS_DISCLAIMER = "ff_settings_disclaimer"
    /**
     * In-app Licenses page (ATNA-116): Settings → Licenses lists third-party
     * libraries/services from assets/licenses/third_party.json. Defaults on
     * (legal / attribution surface for Play norms).
     */
    const val LICENSES_PAGE = "ff_licenses_page"
}

/**
 * Local in-memory flags. Production defaults are off.
 *
 * Optional [onOverrideChanged] is the single write-through hook for durable
 * prefs (ATNA-93): [setEnabled] notifies; [seedIfAbsent] does not (dogfood /
 * structural defaults stay out of the store until explicitly toggled).
 */
class LocalFeatureFlags(
    initial: Map<String, Boolean> = emptyMap(),
    private val onOverrideChanged: ((key: String, enabled: Boolean) -> Unit)? = null,
) : FeatureFlags {
    private val overrides = initial.toMutableMap()

    override fun isEnabled(key: String): Boolean = overrides[key] == true

    /** True when [key] was hydrated or [setEnabled] / [seedIfAbsent] wrote it. */
    fun hasOverride(key: String): Boolean = overrides.containsKey(key)

    fun setEnabled(key: String, enabled: Boolean) {
        overrides[key] = enabled
        onOverrideChanged?.invoke(key, enabled)
    }

    /**
     * In-memory only. Skips when [key] already has an override (including
     * persisted `false`). Does **not** notify [onOverrideChanged].
     */
    fun seedIfAbsent(key: String, enabled: Boolean) {
        if (overrides.containsKey(key)) return
        overrides[key] = enabled
    }
}
