# Map-first home (Phase A)

Feature flag: **`ff_map_home`** (on in debug). Gherkin: [`features/map_first_home.feature`](../features/map_first_home.feature).

## Screens

| Surface | What ships |
| --- | --- |
| **Map home** | Full-bleed MapLibre map (demotiles); camera = **current location** via Play Services **Fused Location Provider** (runtime permission + Settings resolution); Brisbane AU fallback if unavailable |
| **Route drag sheet** | Peek / half / full anchors over the map |
| **Create route** | Optional name; Today / Tomorrow / Pick a date |
| **Add/find stops** | Search field + pickup/load \| drop-off/unload; autocomplete = NoOp stub |
| **Stop list + summary strip** | Ordered stops; summary counts by kind |

Hosted in **MainActivity** Compose **NavHost** only (no new XML / AppCompat screens). Enabling `ff_map_home` also forces the single-activity chrome host.

## Domain

`Route` → `RouteStop[]` with `StopKind.PICKUP_LOAD` \| `DROP_OFF_UNLOAD` (`:core-trips` `RouteSession`).

## Out of scope (later phases)

Live GraphHopper, tolls, load optimisation, verified address provider, offline packs.
