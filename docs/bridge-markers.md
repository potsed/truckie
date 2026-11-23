# Bridge markers on home map

Feature flag: **`ff_bridge_markers`** (on in debug). Gherkin: [`features/bridge_markers.feature`](../features/bridge_markers.feature), [`features/bridge_pack.feature`](../features/bridge_pack.feature).

## Behaviour

| Piece | Detail |
| --- | --- |
| **Surface** | MapLibre map-first home only (`ff_map_home`) |
| **Data (long-term)** | Downloadable, updatable AU **bridge pack** — versioned manifest + JSON body, local cache under app files, refresh when remote version is newer |
| **Data (bootstrap)** | Baked-in sample `app/src/main/assets/bridges/au_sample_bridges.json` seeds the cache when empty (and re-seeds when the bootstrap version advances) — **not** the long-term source of truth |
| **Rendering** | Annotation plugin **SymbolManager** (not deprecated MarkerOptions); separate manager from stop markers; style loaded **once** (concurrent AndroidView updates coalesce — no second `setStyle`); managers recreate if style identity drifts or the manager layer is missing from the live style |
| **Icon** | Australian road sign **W5-8** (low level bridge) — Wikimedia Commons public-domain SVG, rasterised to `res/drawable/au_road_sign_w5_8.png` (~96px @ iconSize ≈ 1.15) |
| **Label** | Short clearance text (e.g. `4.3 m`) when `clearanceHeightM` is present; marker-only otherwise |
| **Where to look** | Sample pack clusters around **Brisbane CBD / SEQ** (within ~2 km of the fallback camera). With **Demo mode** on (`ff_demo_mode`, Settings), use **Jump to sample bridges** on the map chrome to fly there. |
| **Out of scope** | Live CDN hosting, full OSM/NHVR pack ingest, GraphHopper Custom Model, Phase C vehicle UI, create-route changes, global chrome |

Sample points are labelled fixtures for dogfood — not a permit or live restriction feed. Legal disclaimer still applies (`docs/play-console.md`).

## Pack API

- `BridgePackRepository` / `CachedBridgePackRepository` — `loadActive()`, `cachedVersion()`, `refresh(manifest)`
- `BridgePackDownloader` — injectable; production uses `NoOpBridgePackDownloader` until a pack host URL is configured; tests use `FixedPayloadBridgePackDownloader`
- Cache files: `filesDir/bridge_packs/manifest.json` + `bridges.json`

## Catalog

`BridgeMarkersCatalog` (`:app`) gates visibility, parses JSON, builds the amber warning bitmap, and formats clearance labels. Home map loads bridges via `TruckieApp.bridgePackRepository.loadActive()`.
