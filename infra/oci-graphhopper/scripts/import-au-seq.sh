#!/usr/bin/env bash
# Download Geofabrik Australia extract, clip to South East Queensland, load into GraphHopper.
# Idempotent redeploy of graph data. Independent of the Truckie Android app.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STACK_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_DIR="${COMPOSE_DIR:-${STACK_ROOT}/compose}"
# Prefer installed path when scripts live under /opt/oci-graphhopper
if [[ ! -f "${COMPOSE_DIR}/docker-compose.yml" && -f /opt/oci-graphhopper/compose/docker-compose.yml ]]; then
  COMPOSE_DIR=/opt/oci-graphhopper/compose
fi
WORK="${WORK:-/var/tmp/oci-graphhopper-osm}"
# SEQ bbox: Gold Coast → Sunshine Coast coastal corridor (lon_min,lat_min,lon_max,lat_max)
SEQ_BBOX="${SEQ_BBOX:-152.0,-28.55,153.65,-26.35}"
AU_URL="${AU_URL:-https://download.geofabrik.de/australia-oceania/australia-latest.osm.pbf}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--force] [--bbox LON_MIN,LAT_MIN,LON_MAX,LAT_MAX]

Downloads Australia OSM from Geofabrik, clips to SEQ, copies into the
GraphHopper data volume, and recreates the graph-cache.

Environment:
  COMPOSE_DIR  Path to docker-compose.yml (default: ${COMPOSE_DIR})
  SEQ_BBOX     Clip bbox (default: ${SEQ_BBOX})
  AU_URL       Source PBF URL
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force) shift ;; # accepted for operators; always rebuilds graph-cache
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

echo "==> Stopping GraphHopper to swap PBF / wipe graph-cache"
docker compose -f "${COMPOSE_DIR}/docker-compose.yml" stop graphhopper || true

VOL_NAME="$(docker volume ls -q | grep -E 'oci-graphhopper.*gh_data|truckie-gh.*gh_data' | head -n1 || true)"
if [[ -z "${VOL_NAME}" ]]; then
  docker compose -f "${COMPOSE_DIR}/docker-compose.yml" create graphhopper || true
  VOL_NAME="$(docker volume ls -q | grep -E 'oci-graphhopper.*gh_data|truckie-gh.*gh_data' | head -n1)"
fi
if [[ -z "${VOL_NAME}" ]]; then
  echo "Could not find compose volume *gh_data" >&2
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
for _ in $(seq 1 90); do
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
