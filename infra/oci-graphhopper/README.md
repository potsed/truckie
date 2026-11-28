# OCI Free Tier — self-hosted GraphHopper

Reusable, idempotent scaffold to run **open-source GraphHopper** on **Oracle Cloud Free Tier** for Truckie select-user routing (AU/SEQ truck OSM + Custom Model).

**Not** GraphHopper Cloud. **No** Firebase. Packs stay on GitHub Releases.

## Layout

| Path | Purpose |
| --- | --- |
| `terraform/` | OCI network + Always Free Ampere VM + NSG firewall |
| `cloud-init/user-data.yaml` | Docker, Caddy, non-root user, UFW, Compose bootstrap |
| `compose/` | GraphHopper + Caddy (HTTPS / Let’s Encrypt) |
| `scripts/` | OSM import/refresh, healthcheck, local validate |
| `secrets/env.example` | Env template — copy to `secrets/.env` (gitignored) |

## Operator runbook

Canonical steps (console IAM, apply Terraform, wire `GRAPHHOPPER_BASE_URL`):

- Project store: `docs/oci-graphhopper-runbook.md` (coordinator Project Context)
- Repo mirror: [`docs/oci-graphhopper-runbook.md`](../../docs/oci-graphhopper-runbook.md)
- App wiring: [`docs/graphhopper-live.md`](../../docs/graphhopper-live.md)

## Quick path (after Daniel has OCI credentials)

```bash
cd infra/oci-graphhopper
cp secrets/env.example secrets/.env   # fill domain, email, SSH key path — never commit
cp terraform/terraform.tfvars.example terraform/terraform.tfvars
# edit terraform.tfvars with compartment OCID, region, SSH public key
cd terraform && terraform init && terraform plan && terraform apply
```

Daniel must create the Free Tier tenancy / API keys himself. This repo never invents or stores OCI secrets.

## Wire the Android app

```bash
./gradlew :app:assembleDebug \
  -Ptruckie.graphhopper.baseUrl=https://gh.example.com
# or: export TRUCKIE_GRAPHHOPPER_BASE_URL=https://gh.example.com
```

Enable `ff_graphhopper_live` (debug dogfood already does). See `docs/graphhopper-live.md`.
