#!/usr/bin/env bash
# Offline PR gate for infra/oci-valhalla — no OCI credentials required.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAIL=0

check() {
  local name="$1"
  shift
  if "$@"; then
    echo "PASS  ${name}"
  else
    echo "FAIL  ${name}"
    FAIL=1
  fi
}

echo "Validating ${ROOT}"

check "compose project is oci-valhalla" \
  grep -q 'name: oci-valhalla' "${ROOT}/compose/docker-compose.yml"
check "compose uses valhalla-scripted image" \
  grep -q 'valhalla-scripted' "${ROOT}/compose/docker-compose.yml"
check "Valhalla bound to loopback 8002" \
  grep -q '127.0.0.1:8002:8002' "${ROOT}/compose/docker-compose.yml"
check "Caddy proxies /route" \
  grep -q '/route' "${ROOT}/compose/Caddyfile"
check "Caddy proxies /status" \
  grep -q '/status' "${ROOT}/compose/Caddyfile"
check "import script clips SEQ bbox" \
  grep -q '152.0,-28.55,153.65,-26.35' "${ROOT}/scripts/import-qld-seq.sh"
check "import script uses truck costing smoke" \
  grep -q '"costing": "truck"' "${ROOT}/scripts/import-qld-seq.sh"
check "install.sh declares no Android dependency" \
  grep -q 'Does NOT require the Truckie Android app' "${ROOT}/scripts/install.sh"
check "default install root is /opt/oci-valhalla" \
  grep -q '/opt/oci-valhalla' "${ROOT}/scripts/install.sh"
check "tfvars example has valhalla_domain" \
  grep -q 'valhalla_domain' "${ROOT}/terraform/terraform.tfvars.example"
check "outputs hint VALHALLA_BASE_URL" \
  grep -q 'VALHALLA_BASE_URL' "${ROOT}/terraform/outputs.tf"
check "stack sync uses local compose/scripts" \
  grep -qE 'never the|Android app modules' "${ROOT}/terraform/provision.tf"
check "secrets env.example documents VALHALLA_DOMAIN" \
  grep -q 'VALHALLA_DOMAIN' "${ROOT}/secrets/env.example"
check "smoke-brisbane.sh exists" \
  test -f "${ROOT}/scripts/smoke-brisbane.sh"

if grep -qiE 'git clone|gradlew|settings\.gradle|:app:assemble' \
  "${ROOT}/cloud-init/user-data.yaml"; then
  echo "FAIL  cloud-init has no app-build or git-clone steps"
  FAIL=1
else
  echo "PASS  cloud-init has no app-build or git-clone steps"
fi

if grep -q 'gh_repo_ref' "${ROOT}/terraform/variables.tf"; then
  echo "FAIL  no gh_repo_ref in terraform vars"
  FAIL=1
else
  echo "PASS  no gh_repo_ref in terraform vars"
fi

if grep -RInE 'graphhopper|oci-graphhopper|GRAPHHOPPER_' \
  --include='*.sh' --include='*.yaml' --include='*.yml' --include='*.tf' \
  --include='*.example' --include='Caddyfile' \
  "${ROOT}" 2>/dev/null | grep -v 'validate.sh' >/tmp/oci-vh-gh.txt; then
  echo "FAIL  GraphHopper references in stack ops files:"
  cat /tmp/oci-vh-gh.txt
  FAIL=1
else
  echo "PASS  no GraphHopper paths in stack ops files"
fi
rm -f /tmp/oci-vh-gh.txt

if grep -RInE 'settings\.gradle|/app/src/|core-routing|core-flags|:app:assemble' \
  --include='*.sh' --include='*.yaml' --include='*.yml' --include='*.tf' \
  --include='*.example' --include='Caddyfile' \
  "${ROOT}" 2>/dev/null | grep -v 'validate.sh' | grep -v 'Android' >/tmp/oci-vh-indep.txt; then
  echo "FAIL  stack references Android/Gradle paths:"
  cat /tmp/oci-vh-indep.txt
  FAIL=1
else
  echo "PASS  no Android/Gradle module paths in stack ops files"
fi
rm -f /tmp/oci-vh-indep.txt

if [[ "${FAIL}" -ne 0 ]]; then
  echo "Validation failed."
  exit 1
fi
echo "All checks passed."
