# Truckie

Android navigation for vehicles with height, width, length, and mass constraints.
AU-first (QLD/Brisbane), expandable. Product UI term: **Vehicle** (not Truck).

## Stack (Phase 0 scaffold)

| Layer | Choice |
| --- | --- |
| Language | Kotlin **2.4.21** (AGP **9.5.2**, Gradle **9.7.0**) |
| UI | Jetpack Compose Material3 (Compose BOM **2024.12.01**) |
| Modules | `app`, `core-routing`, `core-profiles`, `core-trips`, `core-maps`, `core-flags` |
| Map | MapLibre Native Android (`:core-maps` + `:app` map home, `ff_map_home`) |
| Routing | Valhalla PathRouter (`:core-routing`); interim hosted Valhalla, later self-hosted/offline tiles |
| Flags | Local feature-flag facade (`:core-flags`); durable overrides via Preferences DataStore (`FeatureFlagStore`, ATNA-93) |
| Profiles | Room by rego (`ff_vehicle_profiles`); make/model/year, H/W/L, mass/TARE/GVM/GCM, axles, network |
| Trips | In-memory stop list (`ff_multi_stop`); cargo pick-up/drop-off (`ff_stop_cargo`); optimize Fastest/Cheapest (`ff_route_optimize`) |
| minSdk | **24** (Compose Material3 1.5 / BOM 2026.11; Android Auto gated in code) |

## Clearance model

Effective clearance height after each stop:

```text
max(vehicleHeight, tallestOnBoardCargoHeight) + systemBuffer
```

On-board cargo is simulated in stop order (pick-ups add, drop-offs remove by description).
System buffer is configured in Settings only, clamped to **0.15–1.0 m**.
Cargo height/width/length/mass live on trip stops (pick up / drop off).

## Build

Requires JDK 17+ (21 OK) and an Android SDK (`platforms;android-37.2`, `build-tools;36.0.0`).

### Play Store artifact (primary): AAB

```bash
export ANDROID_HOME=~/Android/Sdk   # or your SDK path
chmod +x scripts/build-aab.sh scripts/build-apk.sh
./scripts/build-aab.sh
# → app/build/outputs/bundle/release/app-release.aab
# equivalent: ./gradlew :app:bundleRelease
```

See [docs/play-console.md](docs/play-console.md) for Play App Signing, targetSdk, privacy, and Android Auto notes.

### Local dogfood: debug APK

```bash
./scripts/build-apk.sh          # debug APK
./gradlew :app:assembleDebug
./gradlew test
```

Debug APK path: `app/build/outputs/apk/debug/app-debug.apk`

**Source-only repo:** `*.apk`, `*.aab`, and Gradle `build/` outputs are gitignored. Never commit binaries or keystores.

## Feature flags

Every user-facing capability is gated. See [docs/feature-flags.md](docs/feature-flags.md).

**Durable prefs (ATNA-93):** Demo mode (Settings) and heading-up lock survive process
death via Preferences DataStore `truckie_feature_flags`. Production defaults stay off;
debug dogfood uses `seedIfAbsent` so persisted toggles are not overwritten.

## Live Valhalla (select-user)

Road-following Navigate / Optimise use **Valhalla truck costing** (`ff_valhalla_live`):

- Interim hosted Valhalla (Simple Routing): [`docs/valhalla-live.md`](docs/valhalla-live.md) (`VALHALLA_API_KEY`)
- Later self-hosted / on-device AU tiles: same client, set `VALHALLA_BASE_URL`, clear API key
- Secrets template: [`secrets.properties.example`](secrets.properties.example)

Self-host notes (docs only): [`docs/valhalla-self-host-notes.md`](docs/valhalla-self-host-notes.md).

Bridge/weight packs stay on GitHub Releases. No Firebase.

Owned G-NAF address pack (QLD-first): [`docs/owned-address-pack.md`](docs/owned-address-pack.md) (`ff_owned_address_pack`).

## Gherkin

One feature file per capability under [`features/`](features/).

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
