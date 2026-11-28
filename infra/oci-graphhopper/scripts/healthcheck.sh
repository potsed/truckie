#!/usr/bin/env bash
# Local + public health probes for monitoring / operators.
set -euo pipefail

DOMAIN="${GRAPHHOPPER_DOMAIN:-}"
LOCAL_URL="${LOCAL_URL:-http://127.0.0.1:8989/health}"
FAIL=0

echo "Local health: ${LOCAL_URL}"
if curl -fsS "${LOCAL_URL}"; then
  echo " OK"
else
  echo " FAIL"
  FAIL=1
fi

if [[ -n "${DOMAIN}" ]]; then
  echo "Public health: https://${DOMAIN}/health"
  if curl -fsS "https://${DOMAIN}/health"; then
    echo " OK"
  else
    echo " FAIL"
    FAIL=1
  fi
fi

exit "${FAIL}"
