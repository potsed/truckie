# Operator runbook — OCI Free Tier Valhalla

Self-hosted **open-source Valhalla** (truck costing) on **Oracle Cloud Always Free**
(Ampere A1.Flex). Permanent path for select-user dogfood / offline independence.
**Not** GraphHopper. **No** Firebase.

**Independence:** deploy/redeploy using only [`infra/oci-valhalla/`](../infra/oci-valhalla/).
No Android modules, no Gradle, no monorepo clone on the VM. The Truckie app is a
consumer of `https://<domain>` via `VALHALLA_BASE_URL`.

**Scaffold:** [`infra/oci-valhalla/`](../infra/oci-valhalla/)  
**App wiring:** [`valhalla-live.md`](./valhalla-live.md)  
**Flag:** `ff_valhalla_live`  
**Catalog:** `OciValhallaHostCatalog` / `features/oci_valhalla_host.feature`

---

## What Daniel must do (credentials are yours)

Agents **do not** invent or store OCI secrets. You supply Free Tier credentials and DNS.

1. Create / sign in to an [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/) tenancy (prefer `ap-sydney-1` or `ap-melbourne-1`).
2. Create an IAM user (or use yours) + API key; note **tenancy OCID**, **user OCID**, **fingerprint**; keep the **private PEM** on your machine only.
3. Create a compartment (e.g. `truckie`) and copy its OCID.
4. Pick a hostname (e.g. `vh.yourdomain.com`) for a DNS **A** record.
5. Generate an SSH keypair (`ssh-ed25519`); password SSH is disabled.
6. Copy `infra/oci-valhalla/terraform/terraform.tfvars.example` → `terraform.tfvars` and fill OCIDs, **SSH public + private key paths**, domain, email.
7. From `infra/oci-valhalla/terraform/`: `terraform init && terraform plan && terraform apply`  
   Apply creates the VM (cloud-init: Docker/UFW/SSH harden), then **syncs only** `compose/`, `scripts/`, `secrets/` via SSH and runs `install.sh`.
8. Point DNS A → Terraform output `public_ip`; wait for Caddy Let’s Encrypt.
9. SSH in and run `/opt/oci-valhalla/scripts/import-qld-seq.sh` (SEQ tiles — Brisbane dogfood).
10. Wire the app: `-Ptruckie.valhalla.baseUrl=https://<domain>` (clear API key); enable `ff_valhalla_live`.

---

## Create / apply / destroy

```bash
cd infra/oci-valhalla/terraform
cp terraform.tfvars.example terraform.tfvars   # edit once
terraform init
terraform plan
terraform apply
terraform output public_ip
terraform output valhalla_base_url_hint
```

Destroy when done dogfooding:

```bash
terraform destroy
```

Re-apply after stack file changes — `null_resource.sync_stack` re-pushes when compose/scripts hashes change.

Manual redeploy without full apply:

```bash
SSH_KEY=~/.ssh/id_ed25519 \
  VALHALLA_DOMAIN=vh.example.com \
  ./infra/oci-valhalla/scripts/sync-to-host.sh ubuntu@<public_ip>
```

Or on the host after copying this directory:

```bash
sudo /opt/oci-valhalla/scripts/install.sh --root /opt/oci-valhalla
```

Offline PR gate:

```bash
./infra/oci-valhalla/scripts/validate.sh
```

---

## First boot checks

```bash
ssh -i ~/.ssh/your_key ubuntu@<public_ip>
sudo tail -f /var/log/cloud-init-output.log   # HOST_READY marker
ls /opt/oci-valhalla/compose                  # present after terraform sync
sudo /opt/oci-valhalla/scripts/import-qld-seq.sh
docker compose -f /opt/oci-valhalla/compose/docker-compose.yml ps
curl -fsS http://127.0.0.1:8002/status
curl -fsS https://<your-domain>/status
```

### Brisbane sample route (curl)

```bash
# Local on the VM
/opt/oci-valhalla/scripts/smoke-brisbane.sh

# Or against the public base URL
VALHALLA_BASE_URL=https://<your-domain> \
  /opt/oci-valhalla/scripts/smoke-brisbane.sh
```

Expect a JSON trip with `trip.legs[].shape` (encoded polyline precision 6).

### Hardening

- UFW: 22/80/443 only; Valhalla on `127.0.0.1:8002`
- SSH: no password, no root login
- Caddy: Let’s Encrypt + HSTS; only `/route`, `/status`
- Secrets: `terraform.tfvars` / `.env` gitignored — never commit PEMs

---

## QLD-first tiles + Free Tier limits

| Step | Detail |
| --- | --- |
| Source | Geofabrik `australia-latest.osm.pbf` |
| Clip | `scripts/import-qld-seq.sh` → SEQ bbox (`--bbox` to widen) |
| Default bbox | `152.0,-28.55,153.65,-26.35` (Gold Coast → Sunshine Coast) |
| Image | `ghcr.io/valhalla/valhalla-scripted:3.9.3` |
| Costing | `truck` (same as `LiveValhallaPathRouter`) |
| Refresh | `scripts/refresh-osm.sh` (monthly cron) |

| Coverage | Fits Always Free (≈12 GB)? | Notes |
| --- | --- | --- |
| **SEQ (shipped default)** | Yes | Brisbane / SEQ truck legs |
| Wider QLD | Maybe | Increase `memory_in_gbs` toward 24; watch tile-build OOM |
| Full AU | Usually **no** | Next step: larger/paid shape, regional packs, or on-device tiles |

Same spirit as G-NAF QLD dogfood slice — ship a working regional path, expand later.

---

## How to get `VALHALLA_BASE_URL`

1. `terraform output valhalla_base_url_hint` → `https://<domain>`
2. Or compose `VALHALLA_DOMAIN` after DNS A → `public_ip`
3. No API key for self-host — clear interim Simple Routing key

### App wiring (BuildConfig)

Any one of (priority: Gradle `-P` → env → `secrets.properties` → `local.properties`):

```bash
# Gradle
./gradlew :app:assembleDebug -Ptruckie.valhalla.baseUrl=https://vh.example.com

# secrets.properties or local.properties (gitignored)
VALHALLA_BASE_URL=https://vh.example.com
VALHALLA_API_KEY=

# Env
TRUCKIE_VALHALLA_BASE_URL=https://vh.example.com
```

Enable `ff_valhalla_live` (debug dogfood defaults on when credentials resolve).
Settings routing status should show **Valhalla self-hosted**.

See [`valhalla-live.md`](./valhalla-live.md). Packs stay on GitHub Releases.

---

## Local compose (no OCI)

```bash
cd infra/oci-valhalla/compose
cp ../secrets/env.example .env
# Clear VALHALLA_DOMAIN for laptop; start Valhalla only
docker compose up -d valhalla
../scripts/import-qld-seq.sh
../scripts/smoke-brisbane.sh http://127.0.0.1:8002
```

Point the app at `http://10.0.2.2:8002` (emulator → host) or your LAN IP — TLS optional for local dogfood.

---

## Monitoring

```bash
VALHALLA_DOMAIN=vh.example.com ./infra/oci-valhalla/scripts/healthcheck.sh
```

---

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Ampere capacity | Other AD or lower `ocpus`/`memory_in_gbs` |
| Stack sync SSH fail | `ssh_private_key_path` matches `ssh_public_key`; security list allows your IP |
| TLS fail | DNS A → `public_ip`; ports 80/443 |
| Valhalla unhealthy / OOM | SEQ clip only; bump RAM toward 24 GB; `docker compose logs valhalla` |
| Tile build stuck | First SEQ build can take 15–45+ min; watch logs |
| Want redeploy without Android | Use `sync-to-host.sh` / `install.sh` only — never need `./gradlew` |
| Full AU needed | Documented next step — not Free Tier default |

---

## Related

- `infra/oci-valhalla/README.md`
- `docs/valhalla-live.md`
- `docs/valhalla-self-host-notes.md`
- `features/oci_valhalla_host.feature`
