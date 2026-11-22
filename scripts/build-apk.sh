#!/usr/bin/env bash
# Build Truckie1 APKs for local dogfood / sideload.
# Play Store path is AAB — prefer ./scripts/build-aab.sh for release uploads.
# Artifacts stay under */build/ — never commit *.apk/*.aab.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VARIANT="${1:-debug}" # debug | release

if [[ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Set ANDROID_HOME (or ANDROID_SDK_ROOT) to your Android SDK." >&2
  exit 1
fi

case "$VARIANT" in
  debug)
    ./gradlew :app:assembleDebug
    APK="app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    ./gradlew :app:assembleRelease
    APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    ;;
  *)
    echo "Usage: $0 [debug|release]" >&2
    exit 1
    ;;
esac

if [[ ! -f "$APK" ]]; then
  echo "Expected APK missing: $APK" >&2
  exit 1
fi

echo "Built: $ROOT/$APK"
ls -lh "$APK"
echo "Note: APK/AAB outputs are gitignored — source only in the repo."
