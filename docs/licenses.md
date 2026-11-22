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
| AU road sign W5-8 (low bridge marker icon) | Public domain | Wikimedia Commons [`AU_road_sign_W5-8.svg`](https://commons.wikimedia.org/wiki/File:AU_road_sign_W5-8.svg) (Standards Australia / AS 1743; PD-ineligible road sign). Bundled SVG + PNG for MapLibre SymbolManager. Attribution not required; source recorded here. |

In-app **Licenses & attribution** screen must ship before public distribution.
Update this file when real dependencies are pinned.
