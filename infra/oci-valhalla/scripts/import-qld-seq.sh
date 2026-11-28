#!/usr/bin/env bash
# Download Geofabrik Australia extract, clip to South East Queensland, build Valhalla tiles.
# Idempotent redeploy of tile data. Independent of the Truckie Android app.
# QLD-first / SEQ dogfood slice — full AU usually exceeds Free Tier RAM (see runbook).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STACK_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_DIR="${COMPOSE_DIR:-${STACK_ROOT}/compose}"
# Prefer installed path when scripts live under /opt/oci-valhalla
if [[ ! -f "${COMPOSE_DIR}/docker-compose.yml" && -f /opt/oci-valhalla/compose/docker-compose.yml ]]; then
  COMPOSE_DIR=/opt/oci-valhalla/compose
fi
WORK="${WORK:-/var/tmp/oci-valhalla-osm}"
# SEQ bbox: Gold Coast → Sunshine Coast coastal corridor (lon_min,lat_min,lon_max,lat_max)
SEQ_BBOX="${SEQ_BBOX:-152.0,-28.55,153.65,-26.35}"
AU_URL="${AU_URL:-https://download.geofabrik.de/australia-oceania/australia-latest.osm.pbf}"
FORCE_REBUILD="${FORCE_REBUILD:-True}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--force] [--bbox LON_MIN,LAT_MIN,LON_MAX,LAT_MAX]

Downloads Australia OSM from Geofabrik, clips to SEQ (QLD-first dogfood),
loads the PBF into the Valhalla custom_files volume, and rebuilds tiles.

Environment:
  COMPOSE_DIR  Path to docker-compose.yml (default: ${COMPOSE_DIR})
  SEQ_BBOX     Clip bbox (default: ${SEQ_BBOX})
  AU_URL       Source PBF URL
  FORCE_REBUILD  Passed to Valhalla container (default: True)

Widen bbox carefully on Free Tier — tile build RAM grows with extract size.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force) FORCE_REBUILD=True; shift ;;
    --bbox) SEQ_BBOX="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing dependency: $1" >&2; exit 1; }; }
need curl
need osmium
need docker

mkdir -p "${WORK}"
cd "${WORK}"

echo "==> Fetching Australia extract"
curl -fL --retry 3 -o australia-latest.osm.pbf "${AU_URL}"

echo "==> Clipping to SEQ bbox ${SEQ_BBOX}"
IFS=',' read -r LEFT BOTTOM RIGHT TOP <<<"${SEQ_BBOX}"
osmium extract --bbox "${LEFT},${BOTTOM},${RIGHT},${TOP}" \
  australia-latest.osm.pbf \
  -o australia-seq.osm.pbf --overwrite

if [[ ! -f "${COMPOSE_DIR}/docker-compose.yml" ]]; then
  echo "COMPOSE_DIR ${COMPOSE_DIR} missing docker-compose.yml — run install.sh first." >&2
  exit 1
fi

echo "==> Stopping Valhalla to swap PBF / wipe tiles"
docker compose -f "${COMPOSE_DIR}/docker-compose.yml" stop valhalla || true

VOL_NAME="$(docker volume ls -q | grep -E 'oci-valhalla.*vh_custom_files|vh_custom_files' | head -n1 || true)"
if [[ -z "${VOL_NAME}" ]]; then
  docker compose -f "${COMPOSE_DIR}/docker-compose.yml" create valhalla || true
  VOL_NAME="$(docker volume ls -q | grep -E 'oci-valhalla.*vh_custom_files|vh_custom_files' | head -n1)"
fi
if [[ -z "${VOL_NAME}" ]]; then
  echo "Could not find compose volume *vh_custom_files" >&2
  exit 1
fi

echo "==> Loading SEQ PBF into volume ${VOL_NAME} (clearing prior tiles)"
docker run --rm \
  -v "${VOL_NAME}:/custom_files" \
  -v "${WORK}/australia-seq.osm.pbf:/in/australia-seq.osm.pbf:ro" \
  alpine:3.20 \
  sh -c 'rm -rf /custom_files/valhalla_tiles /custom_files/valhalla_tiles.tar \
    /custom_files/*.osm.pbf /custom_files/*.md5 2>/dev/null || true; \
    cp /in/australia-seq.osm.pbf /custom_files/australia-seq.osm.pbf'

# Force rebuild on next start
ENV_FILE="${COMPOSE_DIR}/.env"
if [[ -f "${ENV_FILE}" ]]; then
  if grep -q '^FORCE_REBUILD=' "${ENV_FILE}"; then
    sed -i "s|^FORCE_REBUILD=.*|FORCE_REBUILD=${FORCE_REBUILD}|" "${ENV_FILE}"
  else
    echo "FORCE_REBUILD=${FORCE_REBUILD}" >>"${ENV_FILE}"
  fi
  if grep -q '^USE_TILES_IGNORE_PBF=' "${ENV_FILE}"; then
    sed -i 's|^USE_TILES_IGNORE_PBF=.*|USE_TILES_IGNORE_PBF=False|' "${ENV_FILE}"
  else
    echo "USE_TILES_IGNORE_PBF=False" >>"${ENV_FILE}"
  fi
fi

echo "==> Rebuilding Valhalla tiles (first start may take many minutes on Free Tier)"
docker compose -f "${COMPOSE_DIR}/docker-compose.yml" up -d valhalla

echo "==> Waiting for /status"
for _ in $(seq 1 120); do
  if curl -fsS http://127.0.0.1:8002/status >/dev/null 2>&1; then
    echo "Valhalla healthy."
    # Restore ignore-pbf so restarts reuse tiles
    if [[ -f "${ENV_FILE}" ]]; then
      sed -i 's|^FORCE_REBUILD=.*|FORCE_REBUILD=False|' "${ENV_FILE}" || true
      sed -i 's|^USE_TILES_IGNORE_PBF=.*|USE_TILES_IGNORE_PBF=True|' "${ENV_FILE}" || true
    fi
    curl -fsS http://127.0.0.1:8002/status | head -c 400 || true
    echo
    echo "Smoke Brisbane truck route:"
    curl -fsS -X POST 'http://127.0.0.1:8002/route' \
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
      }' | head -c 600 || true
    echo
    exit 0
  fi
  sleep 15
done

echo "Timed out waiting for status — check: docker compose -f ${COMPOSE_DIR}/docker-compose.yml logs valhalla" >&2
exit 1
