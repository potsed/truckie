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
| `ff_demo_mode` | Demo mode — Settings toggle; temporary dogfood affordances (Jump to sample bridges). Fuller demo dataset later, not yet | off (on in debug) |

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
   the default closer zoom (`MapCameraDefaults.ZOOM`).
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
    `assets/bridges/au_sample_bridges.json` only **seeds** an empty cache. Optional short
    clearance labels appear when `clearanceHeightM` is present. Requires map home to be
    the active surface; does not change create-route or vehicle UI.
13. Demo mode (`ff_demo_mode`) is a Settings switch for temporary dogfood affordances.
    Today it gates **Jump to sample bridges** on map-home chrome (also needs
    `ff_bridge_markers`). A fuller demo dataset (prefilled routes/data) may come later —
    not in scope yet. Production defaults remain off.
