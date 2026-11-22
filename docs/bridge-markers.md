# Bridge markers on home map

Feature flag: **`ff_bridge_markers`** (on in debug). Gherkin: [`features/bridge_markers.feature`](../features/bridge_markers.feature), [`features/bridge_pack.feature`](../features/bridge_pack.feature).

## Behaviour

| Piece | Detail |
| --- | --- |
| **Surface** | MapLibre map-first home only (`ff_map_home`) |
| **Data (long-term)** | Downloadable, updatable AU **bridge pack** — versioned manifest + JSON body, local cache under app files, refresh when remote version is newer |
| **Data (bootstrap)** | Baked-in sample `app/src/main/assets/bridges/au_sample_bridges.json` seeds the cache when empty — **not** the long-term source of truth |
| **Rendering** | Annotation plugin **SymbolManager** (not deprecated MarkerOptions); separate manager from stop markers |
| **Label** | Short clearance text (e.g. `4.3 m`) when `clearanceHeightM` is present; marker-only otherwise |
| **Out of scope** | Live CDN hosting in this PR, full OSM/NHVR pack ingest, GraphHopper Custom Model, Phase C vehicle UI, create-route changes, global chrome |

Sample points are labelled fixtures for dogfood — not a permit or live restriction feed. Legal disclaimer still applies (`docs/play-console.md`).

## Pack API

- `BridgePackRepository` / `CachedBridgePackRepository` — `loadActive()`, `cachedVersion()`, `refresh(manifest)`
- `BridgePackDownloader` — injectable; production uses `NoOpBridgePackDownloader` until a pack host URL is configured; tests use `FixedPayloadBridgePackDownloader`
- Cache files: `filesDir/bridge_packs/manifest.json` + `bridges.json`

## Catalog

`BridgeMarkersCatalog` (`:app`) gates visibility, parses JSON, builds the amber warning bitmap, and formats clearance labels. Home map loads bridges via `TruckieApp.bridgePackRepository.loadActive()`.
