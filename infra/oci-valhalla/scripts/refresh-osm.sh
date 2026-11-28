#!/usr/bin/env bash
# Periodic OSM refresh — re-clip Geofabrik AU → SEQ and rebuild Valhalla tiles.
# Independent of the Truckie Android app. Cron-friendly.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_TAG="oci-valhalla-refresh"
COMPOSE_ENV="${COMPOSE_ENV:-/opt/oci-valhalla/compose/.env}"

logger -t "${LOG_TAG}" "Starting OSM refresh"
if "${SCRIPT_DIR}/import-qld-seq.sh" --force "$@"; then
  logger -t "${LOG_TAG}" "OSM refresh succeeded"
  if [[ -f "${COMPOSE_ENV}" ]]; then
    # shellcheck disable=SC1090
    set -a; source "${COMPOSE_ENV}"; set +a
  fi
  if [[ -n "${VALHALLA_DOMAIN:-}" ]]; then
    curl -fsS -X POST "https://${VALHALLA_DOMAIN}/route" \
      -H 'Content-Type: application/json' \
      -d '{
        "locations": [
          {"lat": -27.4698, "lon": 153.0251},
          {"lat": -27.4550, "lon": 153.0350}
        ],
        "costing": "truck",
        "directions_options": {"units": "kilometers"},
        "costing_options": {
          "truck": {"height": 3.5, "width": 2.5, "length": 12.0, "weight": 12.0}
        }
      }' >/tmp/oci-valhalla-smoke.json \
      && logger -t "${LOG_TAG}" "Smoke /route OK" \
      || logger -t "${LOG_TAG}" "Smoke /route failed (TLS/DNS may still be settling)"
  fi
else
  logger -t "${LOG_TAG}" "OSM refresh FAILED"
  exit 1
fi
