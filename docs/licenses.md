# Licenses baseline

Truckie itself is **Apache License 2.0**. See `/LICENSE` and `/NOTICE`.

## Third-party (Phase 0 stubs / planned)

| Component | License | Notes |
| --- | --- | --- |
| MapLibre Native Android (`org.maplibre.gl:android-sdk` **13.7.1**) + Annotation plugin (`android-plugin-annotation` **4.0.0**) | BSD-2-Clause | Wired in `:app`; facade/stub in `:core-maps` |
| GraphHopper | Apache-2.0 | HTTP client stub in `:core-routing` |
| OpenStreetMap data | ODbL | Attribution required in-app |
| AndroidX / Material | Apache-2.0 | Via Maven |
| NHVR / state open data | Varies (often CC-BY) | Overlay pipeline (later) |

In-app **Licenses & attribution** screen must ship before public distribution.
Update this file when real dependencies are pinned.
