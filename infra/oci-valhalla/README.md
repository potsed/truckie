# OCI Free Tier — self-hosted Valhalla

Reusable, **idempotent**, **app-independent** scaffold to run **open-source Valhalla**
(truck costing) on **Oracle Cloud Free Tier** with **QLD-first / SEQ** OSM tiles.

**Not** GraphHopper. **No** Firebase. **No** Android/Gradle required to deploy.

This directory is a standalone ops package. Deploy/redeploy Valhalla using only
`infra/oci-valhalla/` — the Truckie app consumes the resulting HTTPS base URL via
`VALHALLA_BASE_URL` (`ff_valhalla_live`).

## Layout

| Path | Purpose |
| --- | --- |
| `terraform/` | OCI network + Always Free Ampere VM + stack sync provisioner |
| `cloud-init/user-data.yaml` | Host only: Docker, UFW, SSH harden (no monorepo clone) |
| `compose/` | Valhalla (`ghcr.io/valhalla/valhalla-scripted`) + Caddy (HTTPS) |
| `scripts/install.sh` | Idempotent install from this directory |
| `scripts/sync-to-host.sh` | Push this directory to a VM and reinstall |
| `scripts/import-qld-seq.sh` | AU→SEQ OSM clip + Valhalla tile rebuild |
| `secrets/env.example` | Env template — never commit real `.env` |

## Deploy (no Android app)

```bash
cd infra/oci-valhalla
cp secrets/env.example secrets/.env          # optional local notes; gitignored
cp terraform/terraform.tfvars.example terraform/terraform.tfvars
# edit terraform.tfvars: OCIDs, SSH keypair paths, domain, email
cd terraform && terraform init && terraform plan && terraform apply
# apply creates the VM, then syncs compose/scripts via SSH and runs install.sh
```

Redeploy stack files after edits (idempotent):

```bash
SSH_KEY=~/.ssh/id_ed25519 \
  VALHALLA_DOMAIN=vh.example.com \
  ./scripts/sync-to-host.sh ubuntu@<public_ip>

# or on the VM after copying this directory
sudo ./scripts/install.sh --root /opt/oci-valhalla --domain vh.example.com --email you@example.com
```

Then import SEQ tiles (once / on refresh):

```bash
sudo /opt/oci-valhalla/scripts/import-qld-seq.sh
```

Offline gate: `./scripts/validate.sh`

## Local compose (no OCI)

For laptop dogfood without Free Tier:

```bash
cd infra/oci-valhalla/compose
cp ../secrets/env.example .env
# leave VALHALLA_DOMAIN empty → Caddy serves :80 only (or skip Caddy)
docker compose up -d valhalla
../scripts/import-qld-seq.sh
curl -fsS http://127.0.0.1:8002/status
```

## Operator runbook

- Repo: [`docs/oci-valhalla-runbook.md`](../../docs/oci-valhalla-runbook.md)
- App wiring: [`docs/valhalla-live.md`](../../docs/valhalla-live.md)

Daniel supplies Free Tier credentials — this scaffold never invents secrets.

## Point the Truckie app

```bash
./gradlew :app:assembleDebug \
  -Ptruckie.valhalla.baseUrl=https://vh.example.com
# or secrets.properties / local.properties:
# VALHALLA_BASE_URL=https://vh.example.com
# VALHALLA_API_KEY=
```

Enable `ff_valhalla_live`. Packs stay on GitHub Releases.

## Limits (Free Tier)

| Slice | Fits Always Free? | Notes |
| --- | --- | --- |
| **SEQ (default)** | Yes (≈12 GB RAM dogfood) | Brisbane / Gold Coast / Sunshine Coast |
| Wider QLD | Maybe | Widen `--bbox`; watch tile-build RAM |
| Full AU | Usually no | Next step: larger shape / paid Ampere / on-device packs |
