#!/usr/bin/env bash
# Periodic OSM refresh — re-clip Geofabrik AU → SEQ and rebuild graph-cache.
# Intended for cron (e.g. monthly) or manual operator runs.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_TAG="truckie-gh-refresh"

logger -t "${LOG_TAG}" "Starting OSM refresh"
if "${SCRIPT_DIR}/import-au-seq.sh" --force "$@"; then
  logger -t "${LOG_TAG}" "OSM refresh succeeded"
  # Smoke-test a short SEQ truck route if domain is set
  if [[ -f /opt/truckie/compose/.env ]]; then
    # shellcheck disable=SC1091
    set -a; source /opt/truckie/compose/.env; set +a
  fi
  if [[ -n "${GRAPHHOPPER_DOMAIN:-}" ]]; then
    curl -fsS -X POST "https://${GRAPHHOPPER_DOMAIN}/route" \
      -H 'Content-Type: application/json' \
      -d '{
        "profile": "truck",
        "points": [[153.0251,-27.4698],[153.0260,-27.4705]],
        "points_encoded": false,
        "instructions": false
      }' >/tmp/truckie-gh-smoke.json \
      && logger -t "${LOG_TAG}" "Smoke /route OK" \
      || logger -t "${LOG_TAG}" "Smoke /route failed (TLS/DNS may still be settling)"
  fi
else
  logger -t "${LOG_TAG}" "OSM refresh FAILED"
  exit 1
fi
