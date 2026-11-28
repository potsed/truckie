# OCI Free Tier — self-hosted GraphHopper

Reusable, **idempotent**, **app-independent** scaffold to run **open-source GraphHopper** on **Oracle Cloud Free Tier** (AU/SEQ truck OSM + Custom Model).

**Not** GraphHopper Cloud. **No** Firebase. **No** Android/Gradle required to deploy.

This directory is a standalone ops package. Deploy/redeploy GraphHopper using only `infra/oci-graphhopper/` — the Truckie app consumes the resulting HTTPS `/route` URL later via `GRAPHHOPPER_BASE_URL`.

## Layout

| Path | Purpose |
| --- | --- |
| `terraform/` | OCI network + Always Free Ampere VM + stack sync provisioner |
| `cloud-init/user-data.yaml` | Host only: Docker, UFW, SSH harden (no monorepo clone) |
| `compose/` | GraphHopper + Caddy (HTTPS / Let’s Encrypt) |
| `scripts/install.sh` | Idempotent install from this directory |
| `scripts/sync-to-host.sh` | Push this directory to a VM and reinstall |
| `scripts/import-au-seq.sh` | AU→SEQ OSM import / graph rebuild |
| `secrets/env.example` | Env template — never commit real `.env` |

## Deploy (no Android app)

```bash
cd infra/oci-graphhopper
cp secrets/env.example secrets/.env          # optional local notes; gitignored
cp terraform/terraform.tfvars.example terraform/terraform.tfvars
# edit terraform.tfvars: OCIDs, SSH keypair paths, domain, email
cd terraform && terraform init && terraform plan && terraform apply
# apply creates the VM, then syncs compose/scripts via SSH and runs install.sh
```

Redeploy stack files after edits (idempotent):

```bash
# from laptop, against an existing host
SSH_KEY=~/.ssh/id_ed25519 \
  GRAPHHOPPER_DOMAIN=gh.example.com \
  ./scripts/sync-to-host.sh ubuntu@<public_ip>

# or on the VM after copying this directory
sudo ./scripts/install.sh --root /opt/oci-graphhopper --domain gh.example.com --email you@example.com
```

Then import OSM (once / on refresh):

```bash
sudo /opt/oci-graphhopper/scripts/import-au-seq.sh
```

Offline gate: `./scripts/validate.sh`

## Operator runbook

- Project store: `docs/oci-graphhopper-runbook.md`
- Repo: [`docs/oci-graphhopper-runbook.md`](../../docs/oci-graphhopper-runbook.md)

Daniel supplies Free Tier credentials — this scaffold never invents secrets.

## Optional later: point the Truckie app

App wiring is **separate** from deploy:

```bash
./gradlew :app:assembleDebug -Ptruckie.graphhopper.baseUrl=https://gh.example.com
```

See [`docs/graphhopper-live.md`](../../docs/graphhopper-live.md). Packs stay on GitHub Releases.
