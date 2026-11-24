# Map-first home (Phase A)

Feature flags:

| Flag | Role | Default |
| --- | --- | --- |
| `ff_map_home` | Map + sheet | off (on in debug) |
| `ff_map_location_puck` | Blue my-location + follow/recenter (Recenter also restores default closer zoom) | off (on in debug) |
| `ff_map_heading_up` | Map-chrome orientation lock (like Recenter): course-up while following; off = north-up | off (on in debug) |
| `ff_address_autocomplete` | Verified stops | off (on in debug) |
| `ff_route_start_end` | Create route requires verified **start** + **end** (same place = round trip) | off (on in debug) |
| `ff_navigate` | Navigate: in-app GraphHopper path on MapLibre; optional post-route external handoff | off (on in debug) |
| `ff_bridge_markers` | AU bridge markers on MapLibre home (downloadable pack + sample bootstrap) | off (on in debug) |

Gherkin: [`features/map_first_home.feature`](../features/map_first_home.feature), [`features/map_heading_up.feature`](../features/map_heading_up.feature), [`features/create_route_start_end.feature`](../features/create_route_start_end.feature), [`features/bridge_markers.feature`](../features/bridge_markers.feature).

## Screens

| Surface | What ships |
| --- | --- |
| **Map home** | Full-bleed MapLibre map — **OpenFreeMap Liberty** street basemap; camera = **current location** via Fused Location; Brisbane AU fallback; stop markers via SymbolManager; amber **bridge markers** (`ff_bridge_markers`) from versioned pack cache (sample asset seeds empty cache) via a separate SymbolManager (optional clearance label); temporary **Jump to sample bridges** when Demo mode (`ff_demo_mode`) is on; blue **my-location puck** + follow/Recenter when flagged; **orientation lock** (`ff_map_heading_up`) — Recenter and lock share one primary-circle FAB treatment (`MapChromeIconControls`; glyph only differs) |
| **Route drag sheet** | Peek / half / full — Material3 elevated surface, large top radius, drag handle |
| **Create route** | Headline; optional name; **Start** + **End** verified autocomplete when `ff_route_start_end` is on; round-trip checkbox (same place OK); Today / Tomorrow / Pick a date |
| **Navigate** | When `ff_navigate` is on and the active route has start+end: **Navigate** computes a GraphHopper path **in-app** (stub until live HTTP) and draws it on the **same** MapLibre map. Optional Settings handoff app runs **after** that constrained path — never replaces GraphHopper |
| **Add/find stops** | Search + pickup/load \| drop-off/unload chips; verified autocomplete when flagged |
| **Stop list + summary strip** | Numbered timeline; summary includes start→end or round trip when set |

Hosted in **MainActivity** Compose **NavHost** only. Map home suppresses global TopAppBar + bottom NavigationBar.

## Domain

`Route` → verified `start` + `end` (same place = round trip) → `RouteStop[]` with `StopKind.PICKUP_LOAD` \| `DROP_OFF_UNLOAD` (`:core-trips` `RouteSession`).

## Truck routing preferences (Navigate / GraphHopper)

Aligned with architecture-plan §3 Navigate + Phase A/D. Custom Model / path request
shape is recorded by the stub; **live** GraphHopper + bridge/weight packs = Phase D.

- **Route calculation always in-app** on this MapLibre map (never replace with an external nav app)
- Hard constraints: low bridges (`max_height`), weight limits (`max_weight`), toll preference
- Soft residential: prefer non-residential **through-routes**; when a residential delivery
  needs residential roads, keep exposure to **only what is required** to reach the stop
  (minimise penetration — never hard-block delivery access)
- Among safe tours: most **cost-effective and efficient** — Fastest / Cheapest (`optimize`)
- Settings installed-nav app = **post-route handoff only** after the constrained path exists

## Settings

Preference key: `navigation_app_package` (SharedPreferences `truckie_settings`).

| Value | Meaning |
| --- | --- |
| `com.truckie1.in_app` (default) | Stay on Truckie MapLibre after GraphHopper calculates |
| `<installed package>` | Optional **post-route** handoff to that app (destination/waypoints from the calculated path) |

## Related

Verified addresses: [verified-address-autocomplete.md](./verified-address-autocomplete.md). Feature flags: [feature-flags.md](./feature-flags.md).

## Out of scope (later phases)

Live GraphHopper HTTP + full bridge/weight packs (Phase D), toll UX (Phase E), load optimisation, vehicle depth (Phase C), Android Auto.
