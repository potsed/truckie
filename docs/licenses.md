# Licenses

Truckie itself is **Apache License 2.0**. See `/LICENSE` and `/NOTICE`.

## Source of truth

| Layer | Path | Role |
| --- | --- | --- |
| **Runtime / UI SoT** | `app/src/main/assets/licenses/third_party.json` | Curated inventory the in-app **Licenses** page renders |
| **Human mirror** | this file (`docs/licenses.md`) | Docs + PR review; keep in sync when adding deps |

**Rule:** update the JSON first (or in the same PR), then refresh this table. Prefer **accurate curated entries** over a huge dump of every transitive Maven artifact.

In-app: **Settings → Licenses** (`ff_licenses_page`, defaults **on**). Gherkin: `features/licenses_page.feature`.

## Third-party (curated)

| Component | License | Notes |
| --- | --- | --- |
| MapLibre Native Android (`org.maplibre.gl:android-sdk` **13.7.1**) + Annotation plugin (`android-plugin-annotation` **4.0.0**) | BSD-2-Clause | Wired in `:app`; facade/stub in `:core-maps` |
| **OpenFreeMap Liberty** + **OpenStreetMap** data | ODbL (OSM) | Basemap tiles (`tiles.openfreemap.org`). **© OpenStreetMap contributors** — attribution required; MapLibre map attribution stays enabled **and** listed on Licenses |
| Valhalla | MIT | Live PathRouter HTTP client in `:core-routing`. **Interim:** hosted Valhalla; **long-term / offline:** self-hosted or on-device tiles (`docs/valhalla-live.md`, `docs/valhalla-self-host-notes.md`) |
| Geoscape **G-NAF** / G-NAF Core (data.gov.au) | CC-BY 4.0 + G-NAF EULA (no bulk mailing) | Seed for owned AU address pack; cite in pack `source` + Licenses. OK for navigation geocoding — not for bulk mail |
| AndroidX / Jetpack Compose / Material3 / Room / Navigation / DataStore | Apache-2.0 | Via Maven (Compose BOM **2026.11.01**, Room **2.8.5**, …) — curated, not every transitive |
| Google Play Services Location | Play services ToS (free with Play) | Fused Location Provider |
| Kotlin / kotlinx.coroutines | Apache-2.0 | Async routing I/O |
| Nominatim (OSM) | ODbL / Nominatim usage policy | Typeahead UI at most until G-NAF resolves |
| AU road sign W5-8 (low bridge marker icon) | Public domain | Wikimedia Commons [`AU_road_sign_W5-8.svg`](https://commons.wikimedia.org/wiki/File:AU_road_sign_W5-8.svg). Attribution not required; source recorded |

## OSM / OpenFreeMap attribution

Drivers must be able to see OpenStreetMap attribution:

1. **Map** — MapLibre `UiSettings` attribution remains enabled (`LicensesCatalog.mapAttributionRemainsEnabled`).
2. **Licenses page** — OpenFreeMap / OpenStreetMap entry with ODbL and © OpenStreetMap contributors.
