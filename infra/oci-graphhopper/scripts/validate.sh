#!/usr/bin/env bash
# Offline validation of the scaffold (no OCI credentials, no Android SDK).
# Asserts the stack is self-contained and independent of the Truckie app.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAIL=0

check() {
  local desc="$1"
  shift
  if "$@"; then
    echo "PASS  ${desc}"
  else
    echo "FAIL  ${desc}"
    FAIL=1
  fi
}

echo "Validating ${ROOT}"

check "terraform files present" test -f "${ROOT}/terraform/compute.tf"
check "provision.tf present (stack sync)" test -f "${ROOT}/terraform/provision.tf"
check "versions.tf present" test -f "${ROOT}/terraform/versions.tf"
check "tfvars example present" test -f "${ROOT}/terraform/terraform.tfvars.example"
check "cloud-init present" test -f "${ROOT}/cloud-init/user-data.yaml"
check "compose file present" test -f "${ROOT}/compose/docker-compose.yml"
check "Caddyfile present" test -f "${ROOT}/compose/Caddyfile"
check "Dockerfile present" test -f "${ROOT}/compose/Dockerfile"
check "GH config present" test -f "${ROOT}/compose/graphhopper/config.yml"
check "custom model present" test -f "${ROOT}/compose/graphhopper/custom-models/truckie-base.json"
check "env.example present" test -f "${ROOT}/secrets/env.example"
check "install.sh present" test -f "${ROOT}/scripts/install.sh"
check "sync-to-host.sh present" test -f "${ROOT}/scripts/sync-to-host.sh"
check "import script present" test -f "${ROOT}/scripts/import-au-seq.sh"
check "refresh script present" test -f "${ROOT}/scripts/refresh-osm.sh"

for key in GRAPHHOPPER_DOMAIN LETSENCRYPT_EMAIL GH_VERSION JAVA_OPTS; do
  check "env.example has ${key}" grep -q "^${key}=" "${ROOT}/secrets/env.example"
done

if command -v python3 >/dev/null 2>&1; then
  check "truckie-base.json parses" python3 -c "import json; json.load(open('${ROOT}/compose/graphhopper/custom-models/truckie-base.json'))"
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  TMP_ENV="$(mktemp)"
  cp "${ROOT}/secrets/env.example" "${TMP_ENV}"
  check "docker compose config" \
    docker compose --env-file "${TMP_ENV}" -f "${ROOT}/compose/docker-compose.yml" config >/dev/null
  rm -f "${TMP_ENV}"
else
  echo "SKIP  docker compose config (docker not available)"
fi

if command -v terraform >/dev/null 2>&1; then
  check "terraform fmt -check" terraform -chdir="${ROOT}/terraform" fmt -check -recursive
else
  echo "SKIP  terraform fmt (terraform not installed)"
fi

check "secrets/.gitignore denies *" grep -q '^\*$' "${ROOT}/secrets/.gitignore"
check "no committed .env" test ! -f "${ROOT}/secrets/.env"
check "no committed tfvars" test ! -f "${ROOT}/terraform/terraform.tfvars"

check "SSH password auth disabled in cloud-init" \
  grep -q 'PasswordAuthentication no' "${ROOT}/cloud-init/user-data.yaml"
check "GraphHopper bound via localhost in compose" \
  grep -q '127.0.0.1:8989:8989' "${ROOT}/compose/docker-compose.yml"
check "profiles_ch empty for custom models" \
  grep -q 'profiles_ch: \[\]' "${ROOT}/compose/graphhopper/config.yml"

# Independence from Truckie Android app
if grep -qE 'github.com/potsed/truckie|git clone' "${ROOT}/cloud-init/user-data.yaml"; then
  echo "FAIL  cloud-init does not clone truckie monorepo"
  FAIL=1
else
  echo "PASS  cloud-init does not clone truckie monorepo"
fi
if grep -qiE 'gradlew|settings\.gradle|:app:assemble|git clone' "${ROOT}/cloud-init/user-data.yaml"; then
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
if grep -q 'gh_repo_ref' "${ROOT}/terraform/terraform.tfvars.example"; then
  echo "FAIL  tfvars example has no gh_repo_ref"
  FAIL=1
else
  echo "PASS  tfvars example has no gh_repo_ref"
fi
check "install.sh declares no Android dependency" \
  grep -q 'Does NOT require the Truckie Android app' "${ROOT}/scripts/install.sh"
check "stack sync uses local compose/scripts" \
  grep -q 'never the' "${ROOT}/terraform/provision.tf" \
  || grep -q 'Android app modules' "${ROOT}/terraform/provision.tf"
check "default install root is /opt/oci-graphhopper" \
  grep -q '/opt/oci-graphhopper' "${ROOT}/scripts/install.sh"
check "compose project is oci-graphhopper" \
  grep -q 'name: oci-graphhopper' "${ROOT}/compose/docker-compose.yml"

# No accidental Android module paths inside the stack
if grep -RInE 'settings\.gradle|/app/src/|core-routing|core-flags|:app:assemble' \
  --include='*.sh' --include='*.yaml' --include='*.yml' --include='*.tf' \
  --include='*.example' --include='Dockerfile' --include='Caddyfile' \
  "${ROOT}" 2>/dev/null | grep -v 'validate.sh' | grep -v 'Android' >/tmp/oci-gh-indep.txt; then
  echo "FAIL  stack references Android/Gradle paths:"
  cat /tmp/oci-gh-indep.txt
  FAIL=1
else
  echo "PASS  no Android/Gradle module paths in stack ops files"
fi
rm -f /tmp/oci-gh-indep.txt

if [[ "${FAIL}" -ne 0 ]]; then
  echo "Validation failed."
  exit 1
fi
echo "All checks passed."
