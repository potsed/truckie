#!/usr/bin/env bash
# Download Geofabrik Australia extract, clip to South East Queensland, load into GraphHopper.
# Idempotent: skips download if PBF checksum matches; always rebuilds graph when --force.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_DIR="${DATA_DIR:-/var/lib/docker/volumes/truckie-gh_gh_data/_data}"
# Prefer compose volume mount helper when running via docker
COMPOSE_DIR="${COMPOSE_DIR:-/opt/truckie/compose}"
WORK="${WORK:-/var/tmp/truckie-osm}"
# SEQ bbox: Gold Coast → Sunshine Coast coastal corridor (lon_min,lat_min,lon_max,lat_max)
SEQ_BBOX="${SEQ_BBOX:-152.0,-28.55,153.65,-26.35}"
AU_URL="${AU_URL:-https://download.geofabrik.de/australia-oceania/australia-latest.osm.pbf}"
FORCE=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [--force] [--bbox LON_MIN,LAT_MIN,LON_MAX,LAT_MAX]

Downloads Australia OSM from Geofabrik, clips to SEQ, copies into the
GraphHopper data volume, and recreates the graph-cache.

Environment:
  DATA_DIR     Host path to compose volume gh_data (default: docker volume path)
  COMPOSE_DIR  Path to docker-compose.yml (default: /opt/truckie/compose)
  SEQ_BBOX     Clip bbox (default: ${SEQ_BBOX})
  AU_URL       Source PBF URL
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force) FORCE=1; shift ;;
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
# osmium format: left,bottom,right,top
IFS=',' read -r LEFT BOTTOM RIGHT TOP <<<"${SEQ_BBOX}"
osmium extract --bbox "${LEFT},${BOTTOM},${RIGHT},${TOP}" \
  australia-latest.osm.pbf \
  -o australia-seq.osm.pbf --overwrite

if [[ ! -d "${COMPOSE_DIR}" ]]; then
  echo "COMPOSE_DIR ${COMPOSE_DIR} missing — copy compose stack first." >&2
  exit 1
fi

echo "==> Stopping GraphHopper to swap PBF / wipe graph-cache"
docker compose -f "${COMPOSE_DIR}/docker-compose.yml" stop graphhopper || true

# Copy into named volume via a helper container (works without knowing host volume path)
VOL_NAME="$(docker volume ls -q | grep -E 'truckie-gh.*gh_data' | head -n1 || true)"
if [[ -z "${VOL_NAME}" ]]; then
  # Ensure volume exists
  docker compose -f "${COMPOSE_DIR}/docker-compose.yml" create graphhopper || true
  VOL_NAME="$(docker volume ls -q | grep -E 'truckie-gh.*gh_data' | head -n1)"
fi
if [[ -z "${VOL_NAME}" ]]; then
  echo "Could not find compose volume truckie-gh_*gh_data" >&2
  exit 1
fi

echo "==> Loading PBF into volume ${VOL_NAME}"
docker run --rm \
  -v "${VOL_NAME}:/data" \
  -v "${WORK}/australia-seq.osm.pbf:/in/australia-seq.osm.pbf:ro" \
  eclipse-temurin:21-jre-jammy \
  bash -c 'rm -rf /data/graph-cache; mkdir -p /data; cp /in/australia-seq.osm.pbf /data/australia-seq.osm.pbf'

echo "==> Rebuilding graph (first start may take many minutes)"
docker compose -f "${COMPOSE_DIR}/docker-compose.yml" up -d --build graphhopper

echo "==> Waiting for /health"
for i in $(seq 1 90); do
  if curl -fsS http://127.0.0.1:8989/health >/dev/null 2>&1; then
    echo "GraphHopper healthy."
    curl -fsS http://127.0.0.1:8989/info | head -c 400 || true
    echo
    exit 0
  fi
  sleep 10
done

echo "Timed out waiting for health — check: docker compose -f ${COMPOSE_DIR}/docker-compose.yml logs graphhopper" >&2
exit 1
