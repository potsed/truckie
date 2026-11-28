#!/usr/bin/env bash
# Push this stack directory to a running OCI host and re-run install.sh.
# Idempotent redeploy — no Android/Gradle, no full monorepo on the VM.
set -euo pipefail

STACK_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${1:-}"
SSH_KEY="${SSH_KEY:-}"
REMOTE_TMP="${REMOTE_TMP:-/tmp/oci-valhalla-stack}"
INSTALL_ROOT="${INSTALL_ROOT:-/opt/oci-valhalla}"
DOMAIN="${VALHALLA_DOMAIN:-}"
EMAIL="${LETSENCRYPT_EMAIL:-}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [user@]host

Rsyncs infra/oci-valhalla to the host and runs install.sh remotely.

  SSH_KEY=/path/to/key $(basename "$0") ubuntu@203.0.113.10

Optional env: VALHALLA_DOMAIN, LETSENCRYPT_EMAIL, INSTALL_ROOT, SSH_KEY
EOF
}

if [[ -z "${HOST}" || "${HOST}" == "-h" || "${HOST}" == "--help" ]]; then
  usage
  exit $([[ -z "${HOST}" ]] && echo 2 || echo 0)
fi

SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
if [[ -n "${SSH_KEY}" ]]; then
  SSH_OPTS+=(-i "${SSH_KEY}")
fi

echo "==> Sync ${STACK_SRC} → ${HOST}:${REMOTE_TMP}"
ssh "${SSH_OPTS[@]}" "${HOST}" "rm -rf '${REMOTE_TMP}' && mkdir -p '${REMOTE_TMP}'"
rsync -az --delete \
  -e "ssh ${SSH_OPTS[*]}" \
  --exclude 'terraform/.terraform/' \
  --exclude 'terraform/*.tfstate*' \
  --exclude 'terraform/terraform.tfvars' \
  --exclude 'secrets/.env' \
  --exclude 'compose/.env' \
  --exclude '.git/' \
  "${STACK_SRC}/" "${HOST}:${REMOTE_TMP}/"

REMOTE_ARGS=(--root "${INSTALL_ROOT}")
[[ -n "${DOMAIN}" ]] && REMOTE_ARGS+=(--domain "${DOMAIN}")
[[ -n "${EMAIL}" ]] && REMOTE_ARGS+=(--email "${EMAIL}")

echo "==> Remote install"
ssh "${SSH_OPTS[@]}" "${HOST}" \
  "sudo bash '${REMOTE_TMP}/scripts/install.sh' ${REMOTE_ARGS[*]}"

echo "==> Sync+install done. Health: ssh ${HOST} curl -fsS http://127.0.0.1:8002/status"
