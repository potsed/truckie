# Feature flags

Facade: `:core-flags` (`FeatureFlags` / `LocalFeatureFlags`).

Production defaults are **off** until explicitly enabled for dogfood or release.

Presentation stack: **Jetpack Compose Material3** (see `features/compose_ui.feature`). Not a product toggle —
XML screen layouts were removed; do not add new AppCompat/XML screens.

| Flag key | Gates | Default |
| --- | --- | --- |
| `ff_vehicle_profiles` | Vehicle profile list/edit (Room by rego), Home active summary | off (on in debug) |
| `ff_multi_stop` | Trip stops list (reorder / add / delete), Home Plan trip entry | off (on in debug) |
| `ff_stop_cargo` | Cargo H/W/L/mass pick-up/drop-off on stops; on-board clearance | off (on in debug) |
| `ff_bridge_safe_routing` | Height/weight-aware GraphHopper Custom Model routing | off |
| `ff_route_optimize` | Optimize for this vehicle — Fastest / Cheapest among safe tours | off (on in debug) |
| `ff_tolls` | Toll preference stub / avoid-allow settings | off |
| `ff_offline_maps` | Offline map pack manager (MapLibre) | off |
| `ff_android_auto` | Android Auto surfaces (requires API 23+) | off |
| `ff_nhvr_overlay` | NHVR / state restriction overlays | off |
| `ff_nav_chrome` | Material3 TopAppBar + bottom NavigationBar (Home, Plan trip, Vehicles, Settings) | off (on in debug) |
| `ff_map_home` | MapLibre map-first home + route/stop drag sheet (peek/half/full) | off (on in debug) |
| `ff_address_autocomplete` | Verified address autocomplete (Nominatim AU default) for map-home stops | off (on in debug) |
| `ff_map_location_puck` | Blue my-location puck on MapLibre (same Fused fix as camera; subtle LocationComponent pulse; follow mode + Recenter when panned) | off (on in debug) |
| `ff_route_start_end` | Create route requires verified start + end (same place = round trip) on the map-home sheet | off (on in debug) |
| `ff_navigate` | Navigate: GraphHopper path always in-app on MapLibre; Settings may pick installed app for post-route handoff only | off (on in debug) |
| `ff_bridge_markers` | Low-bridge markers on MapLibre home (Australia-first sample JSON asset; SymbolManager + optional clearance label) | off (on in debug) |
| `ff_verified_cbd_clearances` | Verified CBD sample clearances (ATNA-42): labels only for published road heights; unknown / not_applicable omit invented mocks | off (on in debug) |
| `ff_queensland_rail_clearances` | Queensland Rail SEQ road-under-rail clearances (ATNA-44): show published QR railbridges pack entries; off keeps BCC CBD samples only | off (on in debug) |
| `ff_w5_8_low_only` | W5-8 only for low / restricted clearances (ATNA-45): AU W5-8 diamond only when verified ≤ 4.6 m or QR published underbridge | off (on in debug) |
| `ff_tmr_structure_clearances` | TMR Structure Clearances in Queensland (ATNA-46): show published TMR spatial pack entries; off hides TMR rows only | off (on in debug) |
| `ff_bcc_road_height_restrictions` | BCC Road Height Restrictions GIS (ATNA-46): show Brisbane Open Data under-4.6 m local pack entries; off keeps curated CBD samples | off (on in debug) |
| `ff_vehicle_height_bridge_filter` | Home-map W5-8 conflicts only (ATNA-47): show low bridges where clearance &lt; vehicle height + system buffer; no usable height → show all low | off (on in debug) |
| `ff_bridge_passage` | Bridge over vs under (ATNA-48): model under/over/both/unknown; height conflicts block under-paths only (OK to drive over deck) | off (on in debug) |
| `ff_demo_mode` | Demo mode (ATNA-56) — Settings toggle; sample SEQ run (DHL Boronia Rd depot, 6 stops incl. one near a low bridge, 3.5 m vehicle) with hard isolation from real data; also Jump to sample bridges | **off** (including debug — Settings opt-in only) |
| `ff_map_heading_up` | Orient map to driving direction — map-chrome lock control (like Recenter); while following, MapLibre `TRACKING_GPS` course-up (off = north-up `TRACKING`) | off (on in debug) |
| `ff_au_low_bridges_pack` | Consume public AU low-bridges JSON pack (ATNA-50) local-first from GitHub Release when newer | off (on in debug) |

## Rules

1. UI and navigation entry points must check the flag before exposing capability.
2. Unit tests assert gated behavior when flags flip.
3. Android Auto additionally requires `Build.VERSION.SDK_INT >= 23`.
4. Cargo fields and on-board clearance require both `ff_multi_stop` and `ff_stop_cargo`.
5. Route optimize (`ff_route_optimize`) hard-filters by effective clearance (height + buffer) and
   on-board operating mass vs GVM / route max weight. Toll preference stays stubbed as allow-all
   passthrough until `ff_tolls` is implemented.
6. Nav chrome (`ff_nav_chrome`) hosts Material3 `CenterAlignedTopAppBar` + bottom
   `NavigationBar` **once** on `MainActivity` around a Compose NavHost (global fixed
   chrome; content scrolls in between). Plan trip / Vehicles tabs still require
   `ff_multi_stop` / `ff_vehicle_profiles`.
7. Map home (`ff_map_home`) replaces list-first Home with MapLibre + route drag sheet
   inside the same NavHost. Enabling map home also forces the single-activity chrome
   host.
8. Address autocomplete (`ff_address_autocomplete`) requires a verified suggestion
   (formatted label + lat/lng) when adding stops on the map-home sheet. Default
   provider is Nominatim with Australia `countrycodes=au` bias. Free-text-only
   unverified addresses are rejected while the flag is on.
9. Location puck (`ff_map_location_puck`) shows a blue MapLibre LocationComponent with a subtle pulse
   indicator at the driver's Fused Location fix and centres the camera on that same
   fix. Hidden when permission is denied or a fix is unavailable. With `ff_map_home` +
   `ff_map_location_puck`, **follow mode** keeps the puck centred (camera tracks) until
   the driver pans away; a **Recenter** control then restores follow and resets zoom to
   the default closer zoom (`MapCameraDefaults.ZOOM`). Recenter must exit TRACKING
   before re-applying zoom so heading-up SideEffect cannot leave free-pan zoom
   (ATNA-59).
10. Route start/end (`ff_route_start_end`) requires verified autocomplete suggestions for
    Create route start and end. Same place (or round-trip checkbox) is a valid round trip.
11. Navigate (`ff_navigate`) always calculates a GraphHopper path in-app (stub until live)
    with truck preferences (bridges, weight, tolls, soft non-residential through-routes with
    minimised residential penetration, Fastest/Cheapest among safe). Draws on the same
    MapLibre map. Settings key `navigation_app_package` defaults to `com.truckie1.in_app`;
    an installed nav app is optional **post-route** handoff only.
12. Bridge markers (`ff_bridge_markers`) draw Australia-first bridges on the map-first home
    via a separate Annotation **SymbolManager**. Long-term source is a **downloadable
    updatable pack** (`BridgePackRepository`: versioned cache + refresh). The baked-in
    `assets/bridges/au_sample_bridges.json` only **seeds** an empty cache (**local-first**:
    `loadActive` never networks; refresh downloads only when the remote version is newer).
    Demo samples sit on real Brisbane CBD crossings. Optional short clearance labels appear
    when `clearanceHeightM` is present (text-font **Noto Sans Regular** for OpenFreeMap
    Liberty — default Open Sans stack 404s and hides icons). Requires map home to be the
    active surface; does not change create-route or vehicle UI.
13. Verified CBD clearances (`ff_verified_cbd_clearances`, ATNA-42) replace mock demo heights
    with BCC/QR/TMR-sourced road clearances (or omit height as unknown / not_applicable).
    When on, map labels show only `clearanceStatus=verified` heights. Pack meta cites sources
    + retrieval date. Full AU OSM/NHVR ingest remains ATNA-11.
14. Queensland Rail clearances (`ff_queensland_rail_clearances`, ATNA-44) show the 106 published
    SEQ road-under-rail rows from the QR railbridges page (source URL + retrieval date in pack
    meta). Coordinates come from that page’s location maps. When off, QR rows are hidden and
    BCC CBD samples remain. Bootstrap `0.5.0-sample` re-seeds older `-sample` caches without
    refetching on every load.
15. W5-8 low-only (`ff_w5_8_low_only`, ATNA-45): when on, only qualifying low bridges
    get the AU W5-8 symbol — verified height ≤ 4.6 m (`LowBridgeW58.LOW_CLEARANCE_MAX_M`,
    matching QR’s Low clearance &lt;4.6 m band) or QR published underbridges. Unknown /
    not_applicable / high open spans do not get W5-8 or invented heights. TMR/BCC GIS
    rows above 4.6 m stay in the pack for routing but do not get W5-8.
16. TMR Structure Clearances (`ff_tmr_structure_clearances`, ATNA-46) show published TMR
    spatial clearances from data.qld Structure Clearances (StateRoadInformation layers
    21/22; source URL + retrieval date in pack meta). When off, TMR rows are hidden.
17. BCC road height restrictions (`ff_bcc_road_height_restrictions`, ATNA-46) show Brisbane
    Open Data GIS/CSV under-4.6 m local rows. Curated CBD `-sample` river crossings remain
    when the flag is off. Bootstrap `0.7.0-sample` re-seeds older `-sample` caches.
    Physical roadside signs override digital packs; the clearance buffer covers pack lag
    versus signs as well as vehicle variance. NHVR national / other states = ATNA-43.
18. Vehicle-height bridge filter (`ff_vehicle_height_bridge_filter`, ATNA-47): when on
    and an active vehicle has a usable height, home-map W5-8 symbols show only
    conflicts (`clearance < vehicleHeight + ClearanceBuffer`). Keeps W5-8 low-only
    rules; never invents heights. No active vehicle / no usable height → show all
    low-pack markers (`show_all_low`) so Demo Jump still works.
19. Bridge passage (`ff_bridge_passage`, ATNA-48): pack models under/over/both/unknown.
    Height conflicts (and future routing) block driving **under** a restricted
    structure — OK to drive **over** the deck. Bootstrap `0.7.0-sample`. Never
    invents clearances.
20. Demo mode (`ff_demo_mode`, ATNA-56 / ATNA-58) is a Settings switch. When on,
    [DemoModeController] loads [DemoDataset]: DHL Boronia Rd depot, **6 stops**
    (2 pickup / 4 delivery; industrial Yatala/Lytton-style + one residential
    beside QR Colina St / Berrima St Wynnum 1.9 m underbridge for W5-8 dogfood),
    and a **3.5 m** demo vehicle — with **hard isolation** from real vehicles,
    routes, and trip stops (and the reverse when off). Vehicle isolation follows
    the flag directly ([DualModeVehicleProfileRepository]); Home/Vehicles call
    `ensureSeededIfDemo()` so a flag-on session always has the seeded route
    (ATNA-58). Also gates **Jump to sample bridges** on map-home chrome (needs
    `ff_bridge_markers`). Defaults **off** in production **and** debug —
    Settings opt-in only (`DemoModeCatalog.ENABLED_BY_DEFAULT_IN_DEBUG = false`).
21. Map heading-up (`ff_map_heading_up`) is a **map-chrome orientation lock** (same overlay
    pattern as Recenter — not a Settings switch). Requires `ff_map_home` +
    `ff_map_location_puck` follow. Locked → course-up (`TRACKING_GPS` + GPS puck);
    unlocked → north-up `TRACKING`. Free pan exits rotation; Recenter restores follow
    (and course-up when lock is on).
