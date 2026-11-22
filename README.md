# Truckie1

Android navigation for vehicles with height, width, length, and mass constraints.
AU-first (QLD/Brisbane), expandable. Product UI term: **Vehicle** (not Truck).

## Stack (Phase 0 scaffold)

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| Modules | `app`, `core-routing`, `core-profiles`, `core-trips`, `core-maps`, `core-flags` |
| Map | MapLibre stub (`:core-maps`) |
| Routing | GraphHopper client stub (`:core-routing`) |
| Flags | Local feature-flag facade (`:core-flags`) |
| Profiles | Room by rego (`ff_vehicle_profiles`); make/model/year, H/W/L, mass/TARE/GVM/GCM, axles, network |
| minSdk | **16** (Android Auto gated to API **23+**) |

## Clearance model

Effective clearance height:

```text
max(vehicleHeight, tallestCargoHeight) + systemBuffer
```

System buffer is configured in Settings only, clamped to **0.15–1.0 m**.
Cargo height/width/length/mass live on trip stops (pick up / drop off).

## Build

Requires JDK 17+ (21 OK) and an Android SDK (`platforms;android-35`, build-tools).

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

## Gherkin

One feature file per capability under [`features/`](features/).

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
