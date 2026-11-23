# Bridge markers on home map

Feature flags: **`ff_bridge_markers`** (on in debug), **`ff_verified_cbd_clearances`** (on in debug; ATNA-42), **`ff_queensland_rail_clearances`** (on in debug; ATNA-44). Gherkin: [`features/bridge_markers.feature`](../features/bridge_markers.feature), [`features/bridge_pack.feature`](../features/bridge_pack.feature), [`features/verified_cbd_clearances.feature`](../features/verified_cbd_clearances.feature), [`features/queensland_rail_clearances.feature`](../features/queensland_rail_clearances.feature).

## Behaviour

| Piece | Detail |
| --- | --- |
| **Surface** | MapLibre map-first home only (`ff_map_home`) |
| **Data (long-term)** | Downloadable, updatable AU **bridge pack** — versioned manifest + JSON body, local cache under app files, refresh when remote version is newer |
| **Data (bootstrap)** | Baked-in sample `app/src/main/assets/bridges/au_sample_bridges.json` seeds the cache when empty (and re-seeds when the bootstrap version advances) — **not** the long-term source of truth |
| **Local-first** | `loadActive()` is **on-device only** (bundled → disk). No network on cold start, Jump, or map redraw. `refresh()` downloads **only** when the remote version is newer — same/older versions keep the disk pack (power efficiency). Weight packs will follow the same lifecycle later; live OSM/NHVR ingest is ATNA-11 |
| **Rendering** | Annotation plugin **SymbolManager** (not deprecated MarkerOptions); separate manager from stop markers; style loaded **once** (concurrent AndroidView updates coalesce — no second `setStyle`); managers recreate if style identity drifts or the manager layer is missing from the live style |
| **Icon** | Australian road sign **W5-8** (low level bridge) — Wikimedia Commons public-domain SVG, rasterised to `res/drawable/au_road_sign_w5_8.png` (~96px @ iconSize ≈ 1.15) |
| **Label** | Short clearance text (e.g. `3.8 m`) only for **verified** published road heights when `ff_verified_cbd_clearances` is on; marker-only when height is unknown / not applicable. **Must** set `text-font` to **Noto Sans Regular** (OpenFreeMap glyph host); MapLibre’s default Open Sans stack 404s and hides the whole symbol. `textOptional=true` keeps icons if glyphs fail. |
| **Build check** | Debug `versionName` (Android App info / Settings) — Queensland Rail clearances builds are `0.8.9-queensland-rail-clearances` or newer. Prior: `0.8.8-verified-cbd-clearances`, `0.8.7-cbd-samples-local-packs`, `0.8.6-w5-8-openfreemap-fonts`, `0.8.5-bridges-style-lifecycle`. |
| **Where to look** | Demo samples snap onto real **Brisbane CBD crossings** (Story, Victoria, Captain Cook / M3, Goodwill, William Jolly, Go Between, Kurilpa) within ~2 km of the fallback camera. With **`ff_queensland_rail_clearances`**, SEQ **road-under-rail** markers from the QR railbridges page also appear (Ascot, Milton, South Brisbane, …). With **Demo mode** on (`ff_demo_mode`, Settings), use **Jump to sample bridges** on the map chrome to fly to the CBD cluster. |
| **Out of scope** | Live CDN hosting, full OSM/NHVR pack ingest (remaining ATNA-11), GraphHopper Custom Model, Phase C vehicle UI, create-route changes, global chrome |

Sample points are labelled fixtures for dogfood — not a permit or live restriction feed. Legal disclaimer still applies (`docs/play-console.md`).

## Verified CBD clearances (ATNA-42)

Kept in pack bootstrap **`0.5.0-sample`** (still valid after ATNA-44). Provenance: `assets/bridges/au_sample_bridges_meta.json` (sources + retrieval date **2026-11-23**).

| Crossing | Road clearance | Status | Source |
| --- | --- | --- | --- |
| Story Bridge | **3.8 m** | verified | BCC Open Data `road-restrictions` — Kerbside Lane under Bradfield Highway/Story Bridge (`B0001 - 13` / `B0001 - 14`, validation_date 2020-10-08) |
| Victoria Bridge | **4.4 m** | verified | BCC Open Data — South-East Arterial Road under Victoria Bridge (`B0002 - 298`); Cultural Centre Tunnel under Victoria Bridge also 4.4 m (`B0002 - 213`) |
| Captain Cook / M3 | — | unknown | No signed road clearance in BCC ≤4.6 m set or QR SEQ low-rail lists for this river span |
| William Jolly | — | unknown | Same rule |
| Go Between | — | unknown | Same rule (nearby Hale St pedestrian underpass 4.1 m is a different structure) |
| Goodwill | — | not_applicable | Pedestrian/cycle — no vehicle carriageway |
| Kurilpa | — | not_applicable | Pedestrian/cycle — no vehicle carriageway |

## Queensland Rail SEQ clearances (ATNA-44)

Pack bootstrap **`0.5.0-sample`**. Flag **`ff_queensland_rail_clearances`**. Source page (retrieved **2026-11-23**): [Queensland Rail — Low rail bridges in South East Queensland](https://www.queenslandrail.com.au/forcustomers/safetysecurity/railbridges).

- **106** published road-under-rail rows (Low clearance &lt;4.6 m + Clearance 4.6 m to &lt;5.4 m).
- Coordinates taken from each row’s published **Location map** (Google Maps place `3d`/`4d` markers on that page) so W5-8 symbols sit on the real crossings.
- Prefer QR figures over mock/unknown for matching rail assets; **do not** invent heights for bridges absent from the page.
- When the flag is off, QR rows are hidden; BCC CBD river samples still load.

**Clearance policy:** use only published **signed road** vertical clearances (BCC / QR / TMR / NHVR). If none exists for a high open span, omit `clearanceHeightM` and mark `unknown` or `not_applicable`. **Never** invent mock demo heights. **Never** treat Maritime Safety Queensland vessel **air draft above HAT** (Port Procedures – Brisbane) as a truck road clearance.

## Pack lifecycle

1. **Cold start / empty cache** — seed from bundled `au_sample_bridges.json`, stamp bootstrap version (`0.5.0-sample`), write `filesDir/bridge_packs/{manifest,bridges}.json`.
2. **Later loads** — read the on-device cache only (`BridgePackSource.CACHE`). Never re-fetch just because the map recomposed.
3. **App update with newer sample** — if cache version ends with `-sample` and bundled `BOOTSTRAP_VERSION` is newer, re-seed from assets (still no network). Devices on `0.4.0-sample` (or older) re-seed to include Queensland Rail SEQ clearances.
4. **Remote refresh** — call `refresh(manifest)` only when a pack host exists; download runs **iff** `BridgePackVersions.isNewer(remote, cached)`. Failures keep the disk fallback.

## Pack API

- `BridgePackRepository` / `CachedBridgePackRepository` — `loadActive()`, `cachedVersion()`, `refresh(manifest)`
- `BridgePackLifecycle` — local-first contracts (`isLocalFirst`, `loadActiveUsesNetwork = false`, …)
- `VerifiedCbdClearances` — flag gate, provenance loader, label policy
- `QueenslandRailClearances` — QR SEQ flag gate, visibility filter, source URL + retrieval date
- `BridgePackDownloader` — injectable; production uses `NoOpBridgePackDownloader` until a pack host URL is configured; tests use `FixedPayloadBridgePackDownloader` / `CountingBridgePackDownloader`
- Cache files: `filesDir/bridge_packs/manifest.json` + `bridges.json`

## Catalog

`BridgeMarkersCatalog` (`:app`) gates visibility, parses JSON (including `clearanceStatus` / source fields), builds the W5-8 bitmap, and formats clearance labels. Home map loads bridges via `TruckieApp.bridgePackRepository.loadActive()`.
