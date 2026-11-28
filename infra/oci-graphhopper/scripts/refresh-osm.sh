#!/usr/bin/env bash
# Periodic OSM refresh — re-clip Geofabrik AU → SEQ and rebuild graph-cache.
# Independent of the Truckie Android app. Cron-friendly.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_TAG="oci-graphhopper-refresh"
COMPOSE_ENV="${COMPOSE_ENV:-/opt/oci-graphhopper/compose/.env}"

logger -t "${LOG_TAG}" "Starting OSM refresh"
if "${SCRIPT_DIR}/import-au-seq.sh" --force "$@"; then
  logger -t "${LOG_TAG}" "OSM refresh succeeded"
  if [[ -f "${COMPOSE_ENV}" ]]; then
    # shellcheck disable=SC1090
    set -a; source "${COMPOSE_ENV}"; set +a
  fi
  if [[ -n "${GRAPHHOPPER_DOMAIN:-}" ]]; then
    curl -fsS -X POST "https://${GRAPHHOPPER_DOMAIN}/route" \
      -H 'Content-Type: application/json' \
      -d '{
        "profile": "truck",
        "points": [[153.0251,-27.4698],[153.0260,-27.4705]],
        "points_encoded": false,
        "instructions": false
      }' >/tmp/oci-graphhopper-smoke.json \
      && logger -t "${LOG_TAG}" "Smoke /route OK" \
      || logger -t "${LOG_TAG}" "Smoke /route failed (TLS/DNS may still be settling)"
  fi
else
  logger -t "${LOG_TAG}" "OSM refresh FAILED"
  exit 1
fi
