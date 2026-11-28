#!/usr/bin/env bash
# Brisbane CBD → nearby truck /route smoke (local or public).
set -euo pipefail

BASE_URL="${1:-${VALHALLA_BASE_URL:-http://127.0.0.1:8002}}"
BASE_URL="${BASE_URL%/}"

curl -fsS -X POST "${BASE_URL}/route" \
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
  }'
echo
