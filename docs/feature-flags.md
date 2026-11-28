# Feature flags

Facade: `:core-flags` (`FeatureFlags` / `LocalFeatureFlags`).

**Durable prefs (ATNA-93):** flag overrides (including Demo mode and heading-up)
persist through one Preferences DataStore path —
`FeatureFlagStore` / `DataStoreFeatureFlagStore` (`truckie_feature_flags`) —
bound via `FeatureFlagPersistence.bind`. `LocalFeatureFlags.setEnabled` is the
only write-through; `seedIfAbsent` fills debug / structural defaults in memory
without writing the store and never overwrites a persisted override (including
persisted `false`). Production defaults stay **off** until seeded or toggled.
See `features/feature_flag_persist.feature`.

Production defaults are **off** until explicitly enabled for dogfood or release.

Presentation stack: **Jetpack Compose Material3** (see `features/compose_ui.feature`). Not a product toggle —
XML screen layouts were removed; do not add new AppCompat/XML screens.

| Flag key | Gates | Default |
| --- | --- | --- |
| `ff_vehicle_profiles` | Vehicle profile list/edit (Room by rego), Home active summary | off (on in debug) |
| `ff_multi_stop` | Trip stops list (reorder / add / delete), Home Plan trip entry | off (on in debug) |
| `ff_stop_cargo` | Cargo H/W/L/mass pick-up/drop-off on stops; on-board clearance | off (on in debug) |
| `ff_bridge_safe_routing` | Height/weight-aware truck routing model routing (future). Legacy `SafeRouteSelector` deleted (ATNA-92) — production clearance/mass path is MultiStopTourComposer + PathRouter | off |
| `ff_route_optimize` | Optimize for this vehicle — Fastest / Cheapest among safe PathRouter tours (multi-stop stop-order; Trip stops UI) | off (on in debug) |
| `ff_geographic_tour_order` | Geographic tour stop order (ATNA-120): Optimise anchors start+end and permutes only intermediate stops via PathRouter (Valhalla) legs — closer-first / shortest feasible under bridge/weight/toll; avoids city zig-zag. Demo seed is geographic (read-only cannot persist Optimise reorder) | **on** (defaults on; on in debug) |
| `ff_valhalla_live` | Live Valhalla PathRouter (ATNA-113 / ATNA-9): road-network polylines with **truck costing**. **Interim:** hosted Valhalla (Simple Routing default) via `VALHALLA_API_KEY`. **Self-host:** `VALHALLA_BASE_URL` → OCI Free Tier / compose ([`infra/oci-valhalla/`](../infra/oci-valhalla/), [`oci-valhalla-runbook.md`](./oci-valhalla-runbook.md)); clear API key. See `docs/valhalla-live.md`, `secrets.properties.example` | off (on in debug) |
| `ff_path_block_detour` | Alternate around geographic path blocks via PathRouter (ATNA-102): live Valhalla truck costing / exclude polygons; never erase the tour polyline; stub only for tests / missing credentials | **on** (defaults on) |
| `ff_tolls` | Toll preference stub / avoid-allow settings | off |
| `ff_offline_maps` | Offline MapLibre pack manager (ATNA-95): OfflineManager + pack download UX + offline-capable style. **Off in production and debug** until that path ships — see `features/offline_maps.feature`, `OfflineMapsCatalog` | **off** (including debug) |
| `ff_android_auto` | Android Auto surfaces (ATNA-10 / ATNA-95): API 23+ gate via `AndroidAutoGate`; scaffold MessageTemplate today; **NavigationTemplate** + clear next destination required before production enable — see `features/android_auto.feature`, `AndroidAutoCatalog`, `docs/play-console.md` | **off** (including debug) |
| `ff_nhvr_overlay` | NHVR / state restriction overlays (ATNA-11 / ATNA-43 / ATNA-95): National Network Map authority; cite sources; never invent geometries; roadside signs/permits override; not a substitute for bridge packs — see `features/nhvr_overlay.feature`, `NhvrOverlayCatalog` | **off** (including debug) |
| `ff_nav_chrome` | Material3 TopAppBar + bottom NavigationBar (Home, Plan trip, Vehicles, Settings) | off (on in debug) |
| `ff_map_home` | MapLibre map-first home + route/stop drag sheet (peek/half/full) | off (on in debug) |
| `ff_address_autocomplete` | Verified address autocomplete (Nominatim AU default) for map-home stops | off (on in debug) |
| `ff_gnaf_address_geocode` | G-NAF premise geocode (ATNA-108): unit/housenumber points from local G-NAF pack when OSM/Nominatim lacks them; never invents coords | off (on in debug) |
| `ff_map_location_puck` | Blue my-location puck on MapLibre (same Fused fix as camera; subtle LocationComponent pulse; follow mode + Recenter when panned) | off (on in debug) |
| `ff_route_start_end` | Create route requires verified start + end (same place = round trip) on the map-home sheet | off (on in debug) |
| `ff_navigate` | Navigate: PathRouter path always in-app on MapLibre; Settings may pick installed app for post-route handoff only | off (on in debug) |
| `ff_navigate_multi_stop` | Navigate multi-stop tour (ATNA-87): same ordered PathRouter / MultiStopTourComposer geometry as Optimise + map polyline via ActiveTourPath; start→end-only Navigate deleted | **on** (defaults on) |
| `ff_route_path_from_location` | Route path approach (ATNA-99): ActiveTourPath prepends PathRouter leg from fused current location → route start → ordered stops → end; without a fix falls back to start→stops→end | **on** (defaults on) |
| `ff_async_routing_io` | Async Valhalla + Nominatim I/O (ATNA-88 / P1-3): live PathRouter + Nominatim are suspend and hop off Compose main; sync-on-main live HTTP deleted. Does **not** enable `ff_valhalla_live` | **on** (defaults on) |
| `ff_room_route_persist` | Room-persist active route + stops (ATNA-89 / P1-4): Real AppDataSource ActiveTour read/write through Room; demo stays isolated read-only in-memory; `exportSchema` + Migration 1→2; no main-thread Room allow | **on** (defaults on) |
| `ff_active_tour` | Single ActiveTourSession SoT (ATNA-86): map-home Route + Plan Trip / Optimise + demo; TripStop is a projection | **on** (defaults on) |
| `ff_bridge_markers` | Low-bridge markers on MapLibre home (Australia-first sample JSON asset; GeoJSON symbol layer + optional clearance label) | off (on in debug) |
| `ff_stop_markers` | Route stop markers on MapLibre home (ATNA-81 / ATNA-117 / ATNA-121): numbered pickup/drop-off style-image sprites + one symbol layer from active AppDataSource route; shares bridge GeoJSON / style lifecycle bind path | off (on in debug) |
| `ff_map_route_polyline` | Active route polyline on MapLibre home (ATNA-82 / ATNA-117): PathRouter / MultiStopTourComposer constrained geometry via shared style line layer with Navigate; Fastest + Cost Effective checkboxes both on by default (AND); stub geometry only when stub PathRouter is active | off (on in debug) |
| `ff_map_overlay_z_order` | Map overlay z-order (ATNA-122): stop symbol layer on top, route line next, bridges / other overlays below via MapLibre native `Style.addLayerAbove` / `addLayerBelow` (not Annotation zIndex). See `features/map_overlay_z_order.feature` | **on** (defaults on; on in debug) |
| `ff_next_destination_display` | While navigating, show upcoming stop / end address (ATNA-83): compact optional phone chip; **must** show clearly on Android Auto when ATNA-10 lands. Human-driver first-class destination (not turn-only). No route overlay changes | off (on in debug) |
| `ff_verified_cbd_clearances` | Verified CBD sample clearances (ATNA-42): labels only for published road heights; unknown / not_applicable omit invented mocks | off (on in debug) |
| `ff_queensland_rail_clearances` | Queensland Rail SEQ road-under-rail clearances (ATNA-44): show published QR railbridges pack entries; off keeps BCC CBD samples only | off (on in debug) |
| `ff_w5_8_low_only` | W5-8 only for low / restricted clearances (ATNA-45): AU W5-8 diamond only when verified ≤ 4.6 m or QR published underbridge | off (on in debug) |
| `ff_tmr_structure_clearances` | TMR Structure Clearances in Queensland (ATNA-46): show published TMR spatial pack entries; off hides TMR rows only | off (on in debug) |
| `ff_bcc_road_height_restrictions` | BCC Road Height Restrictions GIS (ATNA-46): show Brisbane Open Data under-4.6 m local pack entries; off keeps curated CBD samples | off (on in debug) |
| `ff_vehicle_height_bridge_filter` | Home-map W5-8 conflicts only (ATNA-47): show low bridges where clearance &lt; vehicle height + system buffer; no usable height → show all low | off (on in debug) |
| `ff_bridge_passage` | Bridge over vs under (ATNA-48 / ATNA-73 / ATNA-94): model under/over/both/unknown from pack wire `passage` (no CBD id-name heuristics); shared `BridgePassagePolicy` blocks under-paths only (OK to drive over deck); intentional stop proximity to a low structure is allowed (delivery — demo Wynnum/Colina); mid-path conflicts retry via `ff_path_block_detour` / PathRouter (never blank polyline); home-map uses `BridgeVisibilityPipeline` | off (on in debug) |
| `ff_demo_mode` | Demo mode (ATNA-56 / ATNA-75) — Settings toggle selects the demo `AppDataSource` (same Vehicle/Route/TripStop schema; read-only sample SEQ run). Real data stays in the real store. Floating **DEMO MODE** pill is the only in-app demo label; dataset copy is neutral. Jump FAB + residual helpers removed (ATNA-67 / ATNA-92) | **off** (including debug — Settings opt-in only) |
| `ff_map_heading_up` | Orient map to driving direction — map-chrome lock control (like Recenter); while following, MapLibre `TRACKING_GPS` course-up (off = north-up `TRACKING`) | off (on in debug) |
| `ff_map_street_labels` | Prefer street/road name labels over POI clutter on OpenFreeMap Liberty (ATNA-78): runtime visibility + minzoom tweaks; same OSS style URL | off (on in debug) |
| `ff_au_low_bridges_pack` | Consume public AU low-bridges JSON pack (ATNA-50) local-first from GitHub Release when newer | off (on in debug) |
| `ff_owned_address_pack` | Owned AU address pack SoT (ATNA-109 / ATNA-111): **G-NAF-only** local-first JSON (QLD Kallangur bootstrap → downloadable QLD); one pack/search path. Nominatim = typeahead until G-NAF resolves. See `docs/owned-address-pack.md` | off (on in debug) |
| `ff_settings_disclaimer` | Settings safety disclaimer (ATNA-96): not a permit; roadside signs override packs; verify with NHVR / road authorities; on-board weight is guidance only (not a hard lock). Play / select-user | **on** (defaults on) |
| `ff_licenses_page` | Settings → Licenses (ATNA-116): curated third-party inventory from `assets/licenses/third_party.json`; OSM/OFM attribution also on map. Legal / attribution surface | **on** (defaults on) |

## Architecture (not a separate ship toggle)

Map-home / RootNav / MapLibre orchestration (ATNA-90 / P1-5) is a structural split
gated by existing `ff_map_home` (and related map flags). Domain lives in
`MapHomeController`; `MapLibreHost` is a thin command executor with
`MapLibreBindSnapshot`; route sheet + RootNav destinations are modular files.
See `features/map_home_orchestration.feature`. Live routing is Valhalla only
(`docs/valhalla-live.md`; self-host notes in `docs/valhalla-self-host-notes.md`).

## Deferred Phase G / overlay flags (ATNA-95)

These keys exist so dogfood and select-user work can contract behaviour **before**
enablement. Catalogs encode the contracts; Gherkin lives under `features/`.
**Do not** auto-enable them in `TruckieApp` debug dogfood.

| Flag | Catalog | Gherkin | Notes |
| --- | --- | --- | --- |
| `ff_offline_maps` | `OfflineMapsCatalog` | `features/offline_maps.feature` | Product pack surface = flag **and** engine OfflineManager available. Native engine reports unavailable until OfflineManager lands; JVM stub may register packs for tests. |
| `ff_android_auto` | `AndroidAutoCatalog` / `AndroidAutoGate` | `features/android_auto.feature` | Flag + API ≥ 23. Keep phone screen-on while navigating; phone may blank when Auto connected. Next destination must show clearly on Auto (ATNA-83 → ATNA-10). |
| `ff_nhvr_overlay` | `NhvrOverlayCatalog` | `features/nhvr_overlay.feature` | Overlay guidance only (not a permit). Distinct from `ff_bridge_markers` / AU low-bridges packs. |

## Rules

1. UI and navigation entry points must check the flag before exposing capability.
2. Unit tests assert gated behavior when flags flip.
3. Android Auto additionally requires `Build.VERSION.SDK_INT >= 23` (`AndroidAutoGate` /
   `AndroidAutoCatalog`). Keep `ff_android_auto` off until NavigationTemplate + Auto
   next-destination land (ATNA-10).
4. Cargo fields and on-board clearance require both `ff_multi_stop` and `ff_stop_cargo`.
5. Route optimize (`ff_route_optimize`) hard-filters by effective clearance (height + buffer) and
   on-board operating mass vs GVM / route max weight. Multi-stop tours compose PathRouter legs
   (`MultiStopTourComposer`); under-passage low bridges hard-block when `ff_bridge_passage` is on
   (over deck OK). With `ff_geographic_tour_order` (defaults on, ATNA-120), Optimise anchors
   route start and end and permutes only intermediate stops — geographic tour / closer-first
   under constraints (not raw zig-zag). **Navigate and Optimise share one app-owned `PathRouter`** from
   `ValhallaPathRouterFactory` (`TruckieApp.pathRouter()`). With `ff_navigate_multi_stop`
   (defaults on, ATNA-87), Navigate uses `ActiveTourPath` + ordered composer legs — the same
   geometry as Optimise + map polyline; start→end-only Navigate is deleted. With
   `ff_route_path_from_location` (defaults on, ATNA-99), that geometry prepends a PathRouter
   approach from the fused current location to route start (then stops → end); without a fix
   it falls back to start→stops→end.    Pack bridges map into the composer when
   `ff_bridge_passage` is on. With `ff_path_block_detour` (defaults on, ATNA-102),
   geographic blocks retry via the same PathRouter (live Valhalla truck costing /
   exclude polygons preferred) — never erase the tour line. Live HTTP when
   `ff_async_routing_io` (defaults on) **and** `ff_valhalla_live` + credentials;
   otherwise stub (degraded / tests). Live PathRouter / Nominatim are suspend and
   never called from Compose `remember` (ATNA-88). See `docs/valhalla-live.md`. With `ff_room_route_persist`
   (defaults on, ATNA-89), Real ActiveTour persists routes/stops via Room (`RoomActiveTour`);
   demo stays read-only in-memory; Migration 1→2 + `exportSchema`; production Room builder
   does not `allowMainThreadQueries`. Toll preference stays stubbed as allow-all passthrough
   until `ff_tolls` is implemented. **Fastest** and **Cost Effective** are checkboxes (not
   radio), both on by default — AND selection via `TourOptimizeGoals` / `TourPathSelector`
   (prefer fast and cost-effective together).
6. Nav chrome (`ff_nav_chrome`) hosts Material3 `CenterAlignedTopAppBar` + bottom
   `NavigationBar` **once** on `MainActivity` around a Compose NavHost (global fixed
   chrome; content scrolls in between). Plan trip / Vehicles tabs still require
   `ff_multi_stop` / `ff_vehicle_profiles`. ATNA-74: NavHost-only — legacy redirect
   Activities deleted; `MainActivity` always hosts the shell.
7. Map home (`ff_map_home`) replaces list-first Home with MapLibre + route drag sheet
   inside the same NavHost. When off, list-first Home remains a NavHost destination
   only (no parallel Activity host).
8. Address autocomplete (`ff_address_autocomplete`) requires a verified suggestion
   (formatted label + lat/lng) when adding stops on the map-home sheet. Default
   provider is Nominatim with Australia `countrycodes=au` bias. Free-text-only
   unverified addresses are rejected while the flag is on.
9. Location puck (`ff_map_location_puck`) shows a blue MapLibre LocationComponent with a subtle pulse
   indicator at the driver's Fused Location fix and centres the camera on that same
   fix. Hidden when permission is denied or a fix is unavailable. With `ff_map_home` +
   `ff_map_location_puck`, **follow mode** keeps the puck centred (camera tracks) until
   the driver pans away; a **Recenter** control then restores follow and resets zoom to
   the default closer zoom (`MapCameraDefaults.ZOOM`) via **one** `animateCamera`, then
   enters follow via a **bare** LocationComponent camera-mode set after syncing last
   location (no second zoom; avoids the 0ms timed `setCameraMode` overload that stuck
   tracking — ATNA-61 / ATNA-66). Steady follow leaves the camera to LocationComponent —
   no GPS `moveCamera`. ATNA-69: one [MapFollowReconcile] path owns puck push, follow
   enter/exit, heading sync, and Recenter (no competing SideEffect/GPS/AndroidView
   TRACKING enter paths).
10. Route start/end (`ff_route_start_end`) requires verified autocomplete suggestions for
    Create route start and end. Same place (or round-trip checkbox) is a valid round trip.
11. Navigate (`ff_navigate`) always calculates a PathRouter path in-app (stub until live Valhalla)
    with truck preferences (bridges, weight, tolls, soft non-residential through-routes with
    minimised residential penetration, Fastest/Cheapest among safe). Draws on the same
    MapLibre map. Settings key `navigation_app_package` defaults to `com.truckie1.in_app`;
    an installed nav app is optional **post-route** handoff only.
12. Bridge markers (`ff_bridge_markers`) draw Australia-first bridges on the map-first home
    via a MapLibre **GeoJSONSource** + style **symbol** layer through shared
    [MapStyleOverlayBind] (same path as stop markers / route — ATNA-117; Annotation
    SymbolManager deleted). Long-term source is a **downloadable updatable pack**
    (`BridgePackRepository`: versioned cache + refresh). The baked-in
    `assets/bridges/au_sample_bridges.json` only **seeds** an empty cache (**local-first**:
    `loadActive` never networks; refresh downloads only when the remote version is newer).
    Demo samples sit on real Brisbane CBD crossings. Optional short clearance labels appear
    when `clearanceHeightM` is present (text-font **Noto Sans Regular** for OpenFreeMap
    Liberty — default Open Sans stack 404s and hides icons). Requires map home to be the
    active surface; does not change create-route or vehicle UI.
12b. Stop markers (`ff_stop_markers`, ATNA-81 / ATNA-117 / ATNA-121) plot active-route
    pickup/drop-off stops on the same MapLibre home via the **same** [MapStyleOverlayBind] /
    [MapLibreStyleLifecycle] path as bridges (shared GeoJSON / style-layer path — not a
    parallel stack). Stops come from [AppDataSource] (demo or real). **Numbered style-image
    sprites** (`Style.addImage` + one icon-only symbol layer — no textField, no Annotation).
    Distinct pickup/drop-off colours. Satellite peek is out of scope.
12c. Overlay z-order (`ff_map_overlay_z_order`, ATNA-122): after bind, MapLibre native
    `Style.addLayerAbove` / `addLayerBelow` stack Truckie overlays as **stops top**,
    **route line next**, **bridges / other layers below** (not Annotation zIndex).
    `MapLibreHost.applyOverlays` reconciles after LocationComponent mid-bind.
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
    low-pack markers (`show_all_low`) so CBD samples remain visible without an active vehicle.
19. Bridge passage (`ff_bridge_passage`, ATNA-48 / ATNA-94): pack JSON wire
    `passage` is SoT for under/over/both/unknown (CBD Story/Victoria/Captain Cook
    ids are not inferred). Height conflicts block driving **under** a restricted
    structure — OK to drive **over** the deck. Bootstrap `0.7.0-sample`. Never
    invents clearances.
20. Demo mode (`ff_demo_mode`, ATNA-56 / ATNA-75) is a Settings switch that selects
    the demo [AppDataSource] (same Vehicle / Route / TripStop schema as real).
    [DualModeAppDataSource] swaps stores from the flag — no backup/restore /
    `ensureSeededIfDemo` paths. Demo stores are **read-only**. Dataset labels are
    neutral (e.g. "Airport run", "Rigid 3.5"); the only in-app demo label is the
    floating **DEMO MODE** pill. Sample content: DHL Boronia Rd depot, **6 stops**
    (2 pickup / 4 delivery; industrial + one residential beside QR Colina St /
    Berrima St Wynnum 1.9 m underbridge), **3.5 m** vehicle. Jump FAB + residual
    helpers removed (ATNA-67 / ATNA-92). Defaults **off** in production **and**
    debug — Settings opt-in only (`DemoModeCatalog.ENABLED_BY_DEFAULT_IN_DEBUG = false`).
    **Persists** across process death via the shared [FeatureFlagStore] (ATNA-93) —
    same path as other flag overrides; no separate Demo prefs file.
21. Map heading-up (`ff_map_heading_up`) is a **map-chrome orientation lock** (same overlay
    pattern as Recenter — not a Settings switch). Requires `ff_map_home` +
    `ff_map_location_puck` follow. Locked → course-up (`TRACKING_GPS` + GPS puck);
    unlocked → north-up `TRACKING`. Free pan exits rotation; Recenter restores follow
    (and course-up when lock is on). While already following, orientation lock syncs
    modes via the same [MapFollowReconcile] path (does not exit follow or touch zoom).
    Lock state **persists** via [FeatureFlagStore] (ATNA-93); debug `seedIfAbsent`
    does not overwrite a persisted unlock.
22. Street labels (`ff_map_street_labels`, ATNA-78) keep the OpenFreeMap Liberty style
    URL and apply **runtime** layer visibility / minzoom tweaks after load: ensure
    `highway-name-major` / `highway-name-minor` are visible, lower minor-road name
    minzoom to 14, and hide `poi_transit` (bus stops) plus ranked POI label layers
    (`poi_r1` / `poi_r7` / `poi_r20`). No custom hosted style; OSS free-for-commercial.
23. Map route polyline (`ff_map_route_polyline`, ATNA-82 / ATNA-117) draws the constrained
    active route on MapLibre home via shared GeoJSON LineString + style line layer
    (`MapStyleOverlayBind` / `MapRoutePathStyle`) with Navigate. Geometry from PathRouter /
    MultiStopTourComposer only (optimised tour when cached; else AND tour select with
    Fastest + Cost Effective checkboxes both on). Same under-passage / weight / soft
    residential / over-deck OK / toll-when-flagged constraints as Optimize/Navigate.
    Stub geometry only when stub PathRouter is the active factory. No satellite peek.
24. Next destination display (`ff_next_destination_display`, ATNA-83) shows the upcoming
    stop address (or route end when no intermediate stops) while an in-app Navigate
    path is active. Truckie is for human drivers (not self-driving): destination is
    first-class alongside turn-by-turn. **Phone:** compact single-line optional chip
    (avoid clutter). **Android Auto (ATNA-10):** when Auto navigation lands, the next
    destination **must** show clearly on the car UI — document/contract in
    `NextDestinationDisplayCatalog` + `docs/play-console.md`. Does not touch
    style route overlays / MapRoutePathStyle.
