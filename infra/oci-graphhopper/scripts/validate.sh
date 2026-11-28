#!/usr/bin/env bash
# Offline validation of the scaffold (no OCI credentials required).
# TDD-style gate for infra PRs: structure + example env + compose syntax.
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

check "terraform files present" test -f "${ROOT}/terraform/main.tf" -o -f "${ROOT}/terraform/compute.tf"
check "versions.tf present" test -f "${ROOT}/terraform/versions.tf"
check "tfvars example present" test -f "${ROOT}/terraform/terraform.tfvars.example"
check "cloud-init present" test -f "${ROOT}/cloud-init/user-data.yaml"
check "compose file present" test -f "${ROOT}/compose/docker-compose.yml"
check "Caddyfile present" test -f "${ROOT}/compose/Caddyfile"
check "Dockerfile present" test -f "${ROOT}/compose/Dockerfile"
check "GH config present" test -f "${ROOT}/compose/graphhopper/config.yml"
check "custom model present" test -f "${ROOT}/compose/graphhopper/custom-models/truckie-base.json"
check "env.example present" test -f "${ROOT}/secrets/env.example"
check "import script present" test -x "${ROOT}/scripts/import-au-seq.sh" -o -f "${ROOT}/scripts/import-au-seq.sh"
check "refresh script present" test -f "${ROOT}/scripts/refresh-osm.sh"

# Required env keys documented
for key in GRAPHHOPPER_DOMAIN LETSENCRYPT_EMAIL GH_VERSION JAVA_OPTS; do
  check "env.example has ${key}" grep -q "^${key}=" "${ROOT}/secrets/env.example"
done

# Custom model is valid JSON
if command -v python3 >/dev/null 2>&1; then
  check "truckie-base.json parses" python3 -c "import json; json.load(open('${ROOT}/compose/graphhopper/custom-models/truckie-base.json'))"
fi

# Compose config lint when docker is available
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  # Needs a dummy .env for variable interpolation
  TMP_ENV="$(mktemp)"
  cp "${ROOT}/secrets/env.example" "${TMP_ENV}"
  check "docker compose config" \
    docker compose --env-file "${TMP_ENV}" -f "${ROOT}/compose/docker-compose.yml" config >/dev/null
  rm -f "${TMP_ENV}"
else
  echo "SKIP  docker compose config (docker not available)"
fi

# Terraform fmt/validate when terraform + providers available (optional)
if command -v terraform >/dev/null 2>&1; then
  check "terraform fmt -check" terraform -chdir="${ROOT}/terraform" fmt -check -recursive
else
  echo "SKIP  terraform fmt (terraform not installed)"
fi

# No secrets accidentally committed under secrets/
check "secrets/.gitignore denies *" grep -q '^\*$' "${ROOT}/secrets/.gitignore"
check "no committed .env" test ! -f "${ROOT}/secrets/.env"
check "no committed tfvars" test ! -f "${ROOT}/terraform/terraform.tfvars"

# Hardening markers in cloud-init / Caddy
check "SSH password auth disabled in cloud-init" \
  grep -q 'PasswordAuthentication no' "${ROOT}/cloud-init/user-data.yaml"
check "GraphHopper bound via localhost in compose" \
  grep -q '127.0.0.1:8989:8989' "${ROOT}/compose/docker-compose.yml"
check "profiles_ch empty for custom models" \
  grep -q 'profiles_ch: \[\]' "${ROOT}/compose/graphhopper/config.yml"

if [[ "${FAIL}" -ne 0 ]]; then
  echo "Validation failed."
  exit 1
fi
echo "All checks passed."
