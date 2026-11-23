# Bridge markers on home map

Feature flag: **`ff_bridge_markers`** (on in debug). Gherkin: [`features/bridge_markers.feature`](../features/bridge_markers.feature), [`features/bridge_pack.feature`](../features/bridge_pack.feature).

## Behaviour

| Piece | Detail |
| --- | --- |
| **Surface** | MapLibre map-first home only (`ff_map_home`) |
| **Data (long-term)** | Downloadable, updatable AU **bridge pack** — versioned manifest + JSON body, local cache under app files, refresh when remote version is newer |
| **Data (bootstrap)** | Baked-in sample `app/src/main/assets/bridges/au_sample_bridges.json` seeds the cache when empty (and re-seeds when the bootstrap version advances) — **not** the long-term source of truth |
| **Local-first** | `loadActive()` is **on-device only** (bundled → disk). No network on cold start, Jump, or map redraw. `refresh()` downloads **only** when the remote version is newer — same/older versions keep the disk pack (power efficiency). Weight packs will follow the same lifecycle later; live OSM/NHVR ingest is ATNA-11 |
| **Rendering** | Annotation plugin **SymbolManager** (not deprecated MarkerOptions); separate manager from stop markers; style loaded **once** (concurrent AndroidView updates coalesce — no second `setStyle`); managers recreate if style identity drifts or the manager layer is missing from the live style |
| **Icon** | Australian road sign **W5-8** (low level bridge) — Wikimedia Commons public-domain SVG, rasterised to `res/drawable/au_road_sign_w5_8.png` (~96px @ iconSize ≈ 1.15) |
| **Label** | Short clearance text (e.g. `4.3 m`) when `clearanceHeightM` is present; marker-only otherwise. Demo clearances are **plausible mocks**, not a live permit feed. **Must** set `text-font` to **Noto Sans Regular** (OpenFreeMap glyph host); MapLibre’s default Open Sans stack 404s and hides the whole symbol. `textOptional=true` keeps icons if glyphs fail. |
| **Build check** | Debug `versionName` (Android App info / Settings) — builds with CBD-snapped samples + local-first packs are `0.8.7-cbd-samples-local-packs` or newer. Font fix builds were `0.8.6-w5-8-openfreemap-fonts`; pre-#28 APKs were `0.8.5-bridges-style-lifecycle` or earlier. |
| **Where to look** | Demo samples snap onto real **Brisbane CBD crossings** (Story, Victoria, Captain Cook / M3, Goodwill, William Jolly, Go Between, Kurilpa) within ~2 km of the fallback camera. With **Demo mode** on (`ff_demo_mode`, Settings), use **Jump to sample bridges** on the map chrome to fly there. |
| **Out of scope** | Live CDN hosting, full OSM/NHVR pack ingest (ATNA-11), GraphHopper Custom Model, Phase C vehicle UI, create-route changes, global chrome |

Sample points are labelled fixtures for dogfood — not a permit or live restriction feed. Legal disclaimer still applies (`docs/play-console.md`).

## Pack lifecycle

1. **Cold start / empty cache** — seed from bundled `au_sample_bridges.json`, stamp bootstrap version (`0.3.0-sample`), write `filesDir/bridge_packs/{manifest,bridges}.json`.
2. **Later loads** — read the on-device cache only (`BridgePackSource.CACHE`). Never re-fetch just because the map recomposed.
3. **App update with newer sample** — if cache version ends with `-sample` and bundled `BOOTSTRAP_VERSION` is newer, re-seed from assets (still no network).
4. **Remote refresh** — call `refresh(manifest)` only when a pack host exists; download runs **iff** `BridgePackVersions.isNewer(remote, cached)`. Failures keep the disk fallback.

## Pack API

- `BridgePackRepository` / `CachedBridgePackRepository` — `loadActive()`, `cachedVersion()`, `refresh(manifest)`
- `BridgePackLifecycle` — local-first contracts (`isLocalFirst`, `loadActiveUsesNetwork = false`, …)
- `BridgePackDownloader` — injectable; production uses `NoOpBridgePackDownloader` until a pack host URL is configured; tests use `FixedPayloadBridgePackDownloader` / `CountingBridgePackDownloader`
- Cache files: `filesDir/bridge_packs/manifest.json` + `bridges.json`

## Catalog

`BridgeMarkersCatalog` (`:app`) gates visibility, parses JSON, builds the W5-8 bitmap, and formats clearance labels. Home map loads bridges via `TruckieApp.bridgePackRepository.loadActive()`.
