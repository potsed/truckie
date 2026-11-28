#!/usr/bin/env bash
# Idempotent installer for the OCI GraphHopper stack.
# Self-contained: needs only this directory tree (compose/, scripts/, secrets/).
# Does NOT require the Truckie Android app, Gradle, or a monorepo checkout.
set -euo pipefail

STACK_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INSTALL_ROOT="${INSTALL_ROOT:-/opt/oci-graphhopper}"
DOMAIN="${GRAPHHOPPER_DOMAIN:-}"
EMAIL="${LETSENCRYPT_EMAIL:-}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--root DIR] [--domain FQDN] [--email ADDR]

Install or refresh GraphHopper + Caddy under INSTALL_ROOT from this stack
directory. Safe to re-run (idempotent).

  --root DIR     Install root (default: ${INSTALL_ROOT})
  --domain FQDN  Public hostname for Caddy / Let’s Encrypt
  --email ADDR   Let’s Encrypt contact email

Environment overrides: INSTALL_ROOT, GRAPHHOPPER_DOMAIN, LETSENCRYPT_EMAIL

This script never invokes Gradle, Android SDK, or app modules.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) INSTALL_ROOT="$2"; shift 2 ;;
    --domain) DOMAIN="$2"; shift 2 ;;
    --email) EMAIL="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing dependency: $1" >&2; exit 1; }; }
need docker
need rsync
docker compose version >/dev/null

for required in compose/docker-compose.yml compose/Dockerfile compose/Caddyfile \
  compose/graphhopper/config.yml compose/graphhopper/custom-models/truckie-base.json \
  secrets/env.example; do
  if [[ ! -e "${STACK_SRC}/${required}" ]]; then
    echo "Stack incomplete: missing ${STACK_SRC}/${required}" >&2
    echo "Copy the whole infra/oci-graphhopper directory (not the Android app)." >&2
    exit 1
  fi
done

echo "==> Installing stack from ${STACK_SRC} → ${INSTALL_ROOT}"
install -d -m 0755 "${INSTALL_ROOT}"
install -d -o graphhopper -g graphhopper -m 0750 \
  /var/lib/graphhopper/{data,graph-cache,logs,custom-models} 2>/dev/null \
  || install -d -m 0750 /var/lib/graphhopper/{data,graph-cache,logs,custom-models}

# Preserve operator .env across refreshes; refresh tracked compose/scripts
rsync -a --delete \
  --exclude '.env' \
  --exclude 'terraform/' \
  --exclude 'terraform/**' \
  --exclude '.git/' \
  --exclude '.terraform/' \
  --exclude '*.tfstate*' \
  --exclude 'terraform.tfvars' \
  "${STACK_SRC}/" "${INSTALL_ROOT}/"

ENV_FILE="${INSTALL_ROOT}/compose/.env"
if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${INSTALL_ROOT}/secrets/env.example" "${ENV_FILE}"
fi

set_env() {
  local key="$1" val="$2"
  [[ -z "${val}" ]] && return 0
  if grep -q "^${key}=" "${ENV_FILE}"; then
    sed -i "s|^${key}=.*|${key}=${val}|" "${ENV_FILE}"
  else
    printf '%s=%s\n' "${key}" "${val}" >>"${ENV_FILE}"
  fi
}

set_env GRAPHHOPPER_DOMAIN "${DOMAIN}"
set_env LETSENCRYPT_EMAIL "${EMAIL}"
set_env COMPOSE_PROJECT_NAME oci-graphhopper

cp -a "${INSTALL_ROOT}/compose/graphhopper/custom-models/." /var/lib/graphhopper/custom-models/ || true
chown -R graphhopper:graphhopper /var/lib/graphhopper 2>/dev/null || true

# Symlink convenience: scripts always find compose next to themselves after install
ln -sfn "${INSTALL_ROOT}/compose" /opt/oci-graphhopper-compose 2>/dev/null || true

cd "${INSTALL_ROOT}/compose"
docker compose pull || true
docker compose build || true
# Caddy first; GraphHopper becomes healthy after import-au-seq.sh loads SEQ PBF
docker compose up -d caddy
if docker compose run --rm --no-deps --entrypoint true graphhopper >/dev/null 2>&1; then
  # Start GH only when image builds; it may restart until PBF exists — that is OK
  docker compose up -d graphhopper || true
fi

echo "==> Install complete (idempotent)."
echo "    Compose: ${INSTALL_ROOT}/compose"
echo "    Next (once): ${INSTALL_ROOT}/scripts/import-au-seq.sh"
echo "    Redeploy:    ${INSTALL_ROOT}/scripts/install.sh   # or sync-to-host.sh from laptop"
