# Operator runbook — OCI Free Tier GraphHopper

Self-hosted **open-source GraphHopper** for Truckie select-user routing on **Oracle Cloud Always Free** (Ampere A1.Flex). Not GraphHopper Cloud. No Firebase.

**Scaffold:** [`infra/oci-graphhopper/`](../infra/oci-graphhopper/)  
**App wiring:** [`graphhopper-live.md`](./graphhopper-live.md) (`GRAPHHOPPER_BASE_URL` / BuildConfig)  
**Flag:** `ff_graphhopper_live`

---

## What Daniel must do (credentials are yours)

Agents **do not** invent or store OCI secrets. You supply Free Tier credentials and DNS.

1. Create / sign in to an [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/) tenancy (home region preferably `ap-sydney-1` or `ap-melbourne-1`).
2. Create an IAM user (or use yours) + API key; note **tenancy OCID**, **user OCID**, **fingerprint**, and keep the **private PEM** only on your machine.
3. Create a compartment (e.g. `truckie`) and copy its OCID.
4. Pick a hostname (e.g. `gh.yourdomain.com`) you can point with a DNS **A** record.
5. Generate an SSH keypair (`ssh-ed25519`); password SSH is disabled by cloud-init.
6. Copy `infra/oci-graphhopper/terraform/terraform.tfvars.example` → `terraform.tfvars` and fill OCIDs / keys / domain / email.
7. `terraform init && terraform plan && terraform apply` from `infra/oci-graphhopper/terraform/`.
8. Point DNS A → Terraform output `public_ip`; wait for Caddy Let’s Encrypt.
9. SSH in and run `scripts/import-au-seq.sh` (downloads AU OSM, clips SEQ, builds graph).
10. Build the app with `-Ptruckie.graphhopper.baseUrl=https://<your-domain>` (or `TRUCKIE_GRAPHHOPPER_BASE_URL`) and keep `ff_graphhopper_live` on for dogfood.

---

## IAM (minimum)

| Privilege | Why |
| --- | --- |
| Manage `vcns` / subnets / IGW / security lists in compartment | Network |
| Manage `instance-family` + `volume-family` in compartment | Ampere VM + boot volume |
| Read `instance-images` | Ubuntu 22.04 aarch64 lookup |
| Use API key on the calling user | Terraform provider auth |

Console path (typical): **Identity & Security → Domains → (Default) → Users → API Keys → Add API key**.  
Tenancy OCID: **Governance → Tenancy Details**.  
Compartment: **Identity → Compartments → Create**.

Optional later: store `LETSENCRYPT_EMAIL` / compose `.env` in **OCI Vault** and inject via instance principal — scaffold keeps `.env` on disk gitignored for Free Tier simplicity.

---

## Apply (idempotent)

```bash
cd infra/oci-graphhopper/terraform
cp terraform.tfvars.example terraform.tfvars   # edit
terraform init
terraform plan
terraform apply
terraform output public_ip
```

Re-apply after cloud-init / variable changes. Destroy/recreate the instance if you need a clean boot (`terraform taint oci_core_instance.gh` then apply) — data volume is Docker-named and will be empty on a new host unless you attach block storage (out of scope for v1).

Offline PR gate (no OCI keys):

```bash
./infra/oci-graphhopper/scripts/validate.sh
```

---

## First boot on the VM

```bash
ssh -i ~/.ssh/your_key ubuntu@<public_ip>
# cloud-init may still be running — check:
sudo tail -f /var/log/cloud-init-output.log
# When bootstrap finished:
sudo bash /opt/truckie/repo/infra/oci-graphhopper/scripts/import-au-seq.sh
docker compose -f /opt/truckie/compose/docker-compose.yml ps
curl -fsS http://127.0.0.1:8989/health
curl -fsS https://<your-domain>/health
```

### Hardening already in scaffold

- UFW: 22/80/443 only; GraphHopper published as `127.0.0.1:8989`
- SSH: no password, no root login
- Process user `10001` in container; host system user `graphhopper`
- Caddy: Let’s Encrypt + HSTS; only `/route`, `/health`, `/info` proxied
- Secrets: `secrets/.env` / `terraform.tfvars` gitignored — never commit PEM/OCIDs

---

## AU / SEQ graph + Custom Model

| Step | Detail |
| --- | --- |
| Source | Geofabrik `australia-latest.osm.pbf` |
| Clip | `scripts/import-au-seq.sh` → SEQ bbox (override with `--bbox`) |
| Profile | `truck` in `compose/graphhopper/config.yml` |
| Base model | `compose/graphhopper/custom-models/truckie-base.json` |
| Request model | App `GraphHopperCustomModelStub` (height/weight blocks, soft residential) |
| CH | Disabled (`profiles_ch: []`) so request-time Custom Model works |
| Refresh | `scripts/refresh-osm.sh` (cron monthly recommended) |

Full AU nation-wide extract may exceed Always Free RAM/disk comfortably; **SEQ-first** is the supported Free Tier path. Widen bbox when you upgrade shape/memory.

---

## Wire `GRAPHHOPPER_BASE_URL`

```bash
./gradlew :app:assembleDebug -Ptruckie.graphhopper.baseUrl=https://gh.example.com
# or
export TRUCKIE_GRAPHHOPPER_BASE_URL=https://gh.example.com
```

Writes `BuildConfig.GRAPHHOPPER_BASE_URL`. Factory still needs `ff_graphhopper_live` + `ff_async_routing_io`. See [`graphhopper-live.md`](./graphhopper-live.md).

Bridge/weight **packs** remain on GitHub Releases (no Firebase).

---

## Monitoring (minimal)

```bash
./infra/oci-graphhopper/scripts/healthcheck.sh
# GRAPHHOPPER_DOMAIN=gh.example.com ./infra/oci-graphhopper/scripts/healthcheck.sh
```

Point any uptime checker at `https://<domain>/health`. No Firebase Crashlytics.

---

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Ampere out of capacity | Try other AD (`availability_domain_number`) or reduce `ocpus`/`memory_in_gbs` |
| TLS fails | DNS A must match `GRAPHHOPPER_DOMAIN`; ports 80/443 open |
| `/route` 404 via HTTPS | Caddyfile only proxies `/route*`; confirm compose up |
| GH unhealthy forever | Run import script; `docker compose logs graphhopper` |
| App still stub routing | URL blank or flag off — `GraphHopperLiveCatalog.missingForLiveRouting` |

---

## Related

- [`infra/oci-graphhopper/README.md`](../infra/oci-graphhopper/README.md)
- Project Context inventory: select-user infrastructure / stub inventory (coordinator store)
