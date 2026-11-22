#!/usr/bin/env bash
# Build Truckie Android App Bundle for Play Console upload.
# Artifacts stay under */build/ — never commit *.aab / *.apk (source-only repo).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Set ANDROID_HOME (or ANDROID_SDK_ROOT) to your Android SDK." >&2
  exit 1
fi

./gradlew :app:bundleRelease

AAB="app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "$AAB" ]]; then
  echo "Expected AAB missing: $AAB" >&2
  exit 1
fi

echo "Built (Play upload artifact): $ROOT/$AAB"
ls -lh "$AAB"
echo "Signing: use Play App Signing + an upload keystore locally/CI — never commit keystores."
echo "Note: *.aab / *.apk / build/ outputs are gitignored — source only in the repo."
