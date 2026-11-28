#!/usr/bin/env bash
# Local + public health probes for monitoring / operators.
set -euo pipefail

DOMAIN="${VALHALLA_DOMAIN:-}"
LOCAL_URL="${LOCAL_URL:-http://127.0.0.1:8002/status}"
FAIL=0

echo "Local status: ${LOCAL_URL}"
if curl -fsS "${LOCAL_URL}"; then
  echo " OK"
else
  echo " FAIL"
  FAIL=1
fi

if [[ -n "${DOMAIN}" ]]; then
  echo "Public status: https://${DOMAIN}/status"
  if curl -fsS "https://${DOMAIN}/status"; then
    echo " OK"
  else
    echo " FAIL"
    FAIL=1
  fi
fi

exit "${FAIL}"
