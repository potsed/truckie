# Play Console — high-level requirements (Truckie1)

Primary store artifact: **Android App Bundle (`.aab`)**, not APK.

## Build & upload

```bash
export ANDROID_HOME=~/Android/Sdk
./scripts/build-aab.sh
# → app/build/outputs/bundle/release/app-release.aab
```

Local dogfood can still use a debug APK (`./scripts/build-apk.sh`). Play accepts **AAB** for new apps / updates.

## Signing

1. Create an **upload keystore** locally or in CI secrets (never commit `*.jks` / `*.keystore`).
2. Enable **Play App Signing** in Play Console — Google holds the app signing key; you sign uploads with the upload key.
3. Configure `signingConfigs` in Gradle for release (CI injects store passwords). Unsigned `bundleRelease` is fine for scaffold verification only.

## Target / compile SDK

- Keep `targetSdk` / `compileSdk` at the **current Play requirement** (scaffold uses 35).
- Bump when Play raises the floor; track in release notes.

## Privacy & declarations

- Complete Data safety form (what is collected, why, sharing).
- Privacy policy URL when the app has network accounts, location, or analytics.
- Permissions must match declared use (location for navigation, etc.).
- In-app disclaimer: routing is not a permit — verify with NHVR / road authorities.

## Android Auto (later)

- Auto shipping is **Phase 2+**, gated by `ff_android_auto` and API 23+.
- Navigation category apps need Cars app quality review (NF-* checks, `NavigationTemplate`, intent handling).
- Budget a dedicated Auto compliance milestone before requesting Auto listing.

## Source-only repo

`*.aab`, `*.apk`, and Gradle `build/` outputs stay **gitignored**. Distribute binaries via Play (or CI artifacts), not git.
