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

## Rules

1. UI and navigation entry points must check the flag before exposing capability.
2. Unit tests assert gated behavior when flags flip.
3. Android Auto additionally requires `Build.VERSION.SDK_INT >= 23`.
4. Cargo fields and on-board clearance require both `ff_multi_stop` and `ff_stop_cargo`.
5. Route optimize (`ff_route_optimize`) hard-filters by effective clearance (height + buffer) and
   on-board operating mass vs GVM / route max weight. Toll preference stays stubbed as allow-all
   passthrough until `ff_tolls` is implemented.
6. Nav chrome (`ff_nav_chrome`) adds Material3 `CenterAlignedTopAppBar` + bottom `NavigationBar`
   via a shared Scaffold used by each Activity. Plan trip / Vehicles tabs still require
   `ff_multi_stop` / `ff_vehicle_profiles`.
