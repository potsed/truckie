# Map-first home (Phase A)

Feature flags: **`ff_map_home`** (map + sheet), **`ff_map_location_puck`** (blue my-location indicator + follow/recenter), **`ff_address_autocomplete`** (verified stops) — on in debug. Gherkin: [`features/map_first_home.feature`](../features/map_first_home.feature).

## Screens

| Surface | What ships |
| --- | --- |
| **Map home** | Full-bleed MapLibre map — **OpenFreeMap Liberty** street basemap (`https://tiles.openfreemap.org/styles/liberty`, OSM vector tiles, no API key); camera = **current location** via Play Services **Fused Location Provider** (runtime permission + Settings resolution) at **street-neighbourhood zoom** (`MapCameraDefaults.ZOOM` = 15.5); Brisbane AU fallback if unavailable; stop markers via Annotation **SymbolManager**; blue **my-location puck** (`ff_map_location_puck`) via MapLibre LocationComponent at the same fused fix (camera centres on that fix; subtle repeating pulse ring; accuracy circle optional; hidden without permission); **follow mode** keeps the puck locked at centre while the camera tracks movement; pan exits follow and shows a high-contrast **Recenter** map overlay control |
| **Route drag sheet** | Peek / half / full anchors over the map — Material3 elevated surface, large top radius, high-contrast drag handle ([`route_sheet_polish.feature`](../features/route_sheet_polish.feature)) |
| **Create route** | Headline title; optional name; Today / Tomorrow / Pick a date as outlined selectable cards |
| **Add/find stops** | Search field with leading icon + pickup/load \| drop-off/unload filter chips; verified autocomplete when `ff_address_autocomplete` is on (Nominatim AU); suggestion surface rows |
| **Stop list + summary strip** | Numbered timeline badges + kind labels; title-small summary strip readable over the map |

Hosted in **MainActivity** Compose **NavHost** only (no new XML / AppCompat screens). Enabling `ff_map_home` also forces the single-activity chrome host.

**Chrome:** map home suppresses the global TopAppBar + bottom NavigationBar (map + drag sheet own the viewport). Trip / Vehicles / Settings keep chrome. A map overlay menu opens those destinations.

**Sheet drag:** peek / half / full via `AnchoredDraggable` + Material3 `BottomSheetDefaults.DragHandle` and nested-scroll bridging (not click-only).

## Domain

`Route` → `RouteStop[]` with `StopKind.PICKUP_LOAD` \| `DROP_OFF_UNLOAD` (`:core-trips` `RouteSession`).

## Related

Verified addresses: [verified-address-autocomplete.md](./verified-address-autocomplete.md) (`ff_address_autocomplete`).

## Out of scope (later phases)

Live GraphHopper, tolls, load optimisation, offline packs.
