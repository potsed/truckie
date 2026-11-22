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
