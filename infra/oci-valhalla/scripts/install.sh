#!/usr/bin/env bash
# Idempotent installer for the OCI Valhalla stack.
# Self-contained: needs only this directory tree (compose/, scripts/, secrets/).
# Does NOT require the Truckie Android app, Gradle, or a monorepo checkout.
set -euo pipefail

STACK_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INSTALL_ROOT="${INSTALL_ROOT:-/opt/oci-valhalla}"
DOMAIN="${VALHALLA_DOMAIN:-}"
EMAIL="${LETSENCRYPT_EMAIL:-}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--root DIR] [--domain FQDN] [--email ADDR]

Install or refresh Valhalla + Caddy under INSTALL_ROOT from this stack
directory. Safe to re-run (idempotent).

  --root DIR     Install root (default: ${INSTALL_ROOT})
  --domain FQDN  Public hostname for Caddy / Let’s Encrypt
  --email ADDR   Let’s Encrypt contact email

Environment overrides: INSTALL_ROOT, VALHALLA_DOMAIN, LETSENCRYPT_EMAIL

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

for required in compose/docker-compose.yml compose/Caddyfile secrets/env.example; do
  if [[ ! -e "${STACK_SRC}/${required}" ]]; then
    echo "Stack incomplete: missing ${STACK_SRC}/${required}" >&2
    echo "Copy the whole infra/oci-valhalla directory (not the Android app)." >&2
    exit 1
  fi
done

echo "==> Installing stack from ${STACK_SRC} → ${INSTALL_ROOT}"
install -d -m 0755 "${INSTALL_ROOT}"
install -d -o valhalla -g valhalla -m 0750 \
  /var/lib/valhalla/{custom_files,logs} 2>/dev/null \
  || install -d -m 0750 /var/lib/valhalla/{custom_files,logs}

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

set_env VALHALLA_DOMAIN "${DOMAIN}"
set_env LETSENCRYPT_EMAIL "${EMAIL}"
set_env COMPOSE_PROJECT_NAME oci-valhalla

chown -R valhalla:valhalla /var/lib/valhalla 2>/dev/null || true

ln -sfn "${INSTALL_ROOT}/compose" /opt/oci-valhalla-compose 2>/dev/null || true

cd "${INSTALL_ROOT}/compose"
docker compose pull || true

# Enable Caddy TLS profile when a domain is configured
COMPOSE_ARGS=(up -d)
if [[ -n "${DOMAIN}" ]]; then
  COMPOSE_ARGS=(--profile tls up -d)
fi
docker compose "${COMPOSE_ARGS[@]}" valhalla || true
if [[ -n "${DOMAIN}" ]]; then
  docker compose --profile tls up -d caddy || true
fi

echo "==> Install complete (idempotent)."
echo "    Compose: ${INSTALL_ROOT}/compose"
echo "    Next (once): ${INSTALL_ROOT}/scripts/import-qld-seq.sh"
echo "    Redeploy:    ${INSTALL_ROOT}/scripts/install.sh   # or sync-to-host.sh from laptop"
