# Operator runbook — OCI Free Tier GraphHopper

Self-hosted **open-source GraphHopper** on **Oracle Cloud Always Free** (Ampere A1.Flex). Not GraphHopper Cloud. No Firebase.

**Independence:** deploy/redeploy using only [`infra/oci-graphhopper/`](../infra/oci-graphhopper/). No Android modules, no Gradle, no monorepo clone on the VM. The Truckie app is an optional later consumer of `https://<domain>/route`.

**Scaffold:** [`infra/oci-graphhopper/`](../infra/oci-graphhopper/)  
**App wiring (optional, separate):** [`graphhopper-live.md`](./graphhopper-live.md)  
**Flag (app only):** `ff_graphhopper_live`

---

## What Daniel must do (credentials are yours)

Agents **do not** invent or store OCI secrets. You supply Free Tier credentials and DNS.

1. Create / sign in to an [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/) tenancy (prefer `ap-sydney-1` or `ap-melbourne-1`).
2. Create an IAM user (or use yours) + API key; note **tenancy OCID**, **user OCID**, **fingerprint**; keep the **private PEM** on your machine only.
3. Create a compartment (e.g. `truckie`) and copy its OCID.
4. Pick a hostname (e.g. `gh.yourdomain.com`) for a DNS **A** record.
5. Generate an SSH keypair (`ssh-ed25519`); password SSH is disabled.
6. Copy `infra/oci-graphhopper/terraform/terraform.tfvars.example` → `terraform.tfvars` and fill OCIDs, **SSH public + private key paths**, domain, email.
7. From `infra/oci-graphhopper/terraform/`: `terraform init && terraform plan && terraform apply`  
   Apply creates the VM (cloud-init: Docker/UFW/SSH harden), then **syncs only** `compose/`, `scripts/`, `secrets/` via SSH and runs `install.sh`.
8. Point DNS A → Terraform output `public_ip`; wait for Caddy Let’s Encrypt.
9. SSH in and run `/opt/oci-graphhopper/scripts/import-au-seq.sh`.
10. *(Optional, app)* Build Truckie with `-Ptruckie.graphhopper.baseUrl=https://<domain>` when ready to dogfood live routing.

---

## IAM (minimum)

| Privilege | Why |
| --- | --- |
| Manage `vcns` / subnets / IGW / security lists in compartment | Network |
| Manage `instance-family` + `volume-family` in compartment | Ampere VM + boot volume |
| Read `instance-images` | Ubuntu 22.04 aarch64 lookup |
| Use API key on the calling user | Terraform provider auth |

Console: **Identity & Security → Domains → Users → API Keys**.  
Tenancy OCID: **Governance → Tenancy Details**.  
Compartment: **Identity → Compartments → Create**.

---

## Apply / redeploy (idempotent)

```bash
cd infra/oci-graphhopper/terraform
cp terraform.tfvars.example terraform.tfvars   # edit once
terraform init
terraform plan
terraform apply
terraform output public_ip
```

Re-apply after stack file changes — `null_resource.sync_stack` re-pushes when compose/scripts hashes change.

Manual redeploy without full apply:

```bash
SSH_KEY=~/.ssh/id_ed25519 \
  GRAPHHOPPER_DOMAIN=gh.example.com \
  ./infra/oci-graphhopper/scripts/sync-to-host.sh ubuntu@<public_ip>
```

Or on the host after copying this directory:

```bash
sudo /opt/oci-graphhopper/scripts/install.sh --root /opt/oci-graphhopper
```

Set `enable_stack_sync = false` if you prefer sync-to-host only (still need `ssh_private_key_path` unset path OK when sync disabled).

Offline PR gate:

```bash
./infra/oci-graphhopper/scripts/validate.sh
```

---

## First boot checks

```bash
ssh -i ~/.ssh/your_key ubuntu@<public_ip>
sudo tail -f /var/log/cloud-init-output.log   # HOST_READY marker
ls /opt/oci-graphhopper/compose              # present after terraform sync
sudo /opt/oci-graphhopper/scripts/import-au-seq.sh
docker compose -f /opt/oci-graphhopper/compose/docker-compose.yml ps
curl -fsS http://127.0.0.1:8989/health
curl -fsS https://<your-domain>/health
```

### Hardening

- UFW: 22/80/443 only; GraphHopper on `127.0.0.1:8989`
- SSH: no password, no root login
- Container user `10001`; host user `graphhopper`
- Caddy: Let’s Encrypt + HSTS; only `/route`, `/health`, `/info`
- Secrets: `terraform.tfvars` / `.env` gitignored — never commit PEMs

---

## AU / SEQ graph + Custom Model

| Step | Detail |
| --- | --- |
| Source | Geofabrik `australia-latest.osm.pbf` |
| Clip | `scripts/import-au-seq.sh` → SEQ bbox (`--bbox` to widen) |
| Profile | `truck` in `compose/graphhopper/config.yml` |
| Base model | `compose/graphhopper/custom-models/truckie-base.json` |
| Request model | App `GraphHopperCustomModelStub` (when app is wired) |
| CH | Disabled (`profiles_ch: []`) for request-time Custom Model |
| Refresh | `scripts/refresh-osm.sh` (monthly cron) |

SEQ-first is the Free Tier path.

---

## Optional: wire `GRAPHHOPPER_BASE_URL` (app)

Not required for the GH host to run.

```bash
./gradlew :app:assembleDebug -Ptruckie.graphhopper.baseUrl=https://gh.example.com
```

See [`graphhopper-live.md`](./graphhopper-live.md). Packs stay on GitHub Releases.

---

## Monitoring

```bash
GRAPHHOPPER_DOMAIN=gh.example.com ./infra/oci-graphhopper/scripts/healthcheck.sh
```

---

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Ampere capacity | Other AD or lower `ocpus`/`memory_in_gbs` |
| Stack sync SSH fail | `ssh_private_key_path` matches `ssh_public_key`; security list allows your IP |
| TLS fail | DNS A → `public_ip`; ports 80/443 |
| GH unhealthy | Run `import-au-seq.sh`; `docker compose logs graphhopper` |
| Want redeploy without Android | Use `sync-to-host.sh` / `install.sh` only — never need `./gradlew` |
