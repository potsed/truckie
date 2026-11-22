# Bridge markers on home map

Feature flag: **`ff_bridge_markers`** (on in debug). Gherkin: [`features/bridge_markers.feature`](../features/bridge_markers.feature).

## Behaviour

| Piece | Detail |
| --- | --- |
| **Surface** | MapLibre map-first home only (`ff_map_home`) |
| **Data** | Local Australia-first fixture: `app/src/main/assets/bridges/au_sample_bridges.json` |
| **Rendering** | Annotation plugin **SymbolManager** (not deprecated MarkerOptions); separate manager from stop markers |
| **Label** | Short clearance text (e.g. `4.3 m`) when `clearanceHeightM` is present; marker-only otherwise |
| **Out of scope** | Live GraphHopper, full OSM/NHVR pack ingest, Phase C vehicle UI, create-route changes, global chrome |

Sample points are labelled fixtures for dogfood — not a permit or live restriction feed. Legal disclaimer still applies (`docs/play-console.md`).

## Catalog

`BridgeMarkersCatalog` (`:app`) gates visibility, parses JSON, builds the amber warning bitmap, and formats clearance labels.
