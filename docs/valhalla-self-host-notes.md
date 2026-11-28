# Valhalla self-host / OCI notes

Truckie’s **sole** live PathRouter is Valhalla (`ff_valhalla_live` /
[LiveValhallaPathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/LiveValhallaPathRouter.kt)).

**Working path (ATNA-9):** reusable OCI Free Tier + local Compose scaffold under
[`infra/oci-valhalla/`](../infra/oci-valhalla/). Operator runbook:
[`oci-valhalla-runbook.md`](./oci-valhalla-runbook.md).

GraphHopper OCI (`infra/oci-graphhopper/`) stays **deleted** (ATNA-114) — do not revive.

## App wiring (in-tree)

| Piece | Where |
| --- | --- |
| Flag | `ff_valhalla_live` |
| Catalog | `ValhallaLiveCatalog` / `OciValhallaHostCatalog` |
| Factory | `ValhallaPathRouterFactory` |
| Interim hosted | `VALHALLA_API_KEY` → Simple Routing default base |
| Self-host / offline | `VALHALLA_BASE_URL` serving `/route`; **clear** API key |

See [`valhalla-live.md`](./valhalla-live.md) for credentials and request shape.

## Operator path

1. Deploy [`infra/oci-valhalla/`](../infra/oci-valhalla/) (Terraform + `import-qld-seq.sh`) **or** local `docker compose`.
2. SEQ (QLD-first) tiles are the Free Tier default — Brisbane sample smoke included.
3. Point Truckie at the host:
   - `truckie.valhalla.baseUrl=https://<domain>` or `TRUCKIE_VALHALLA_BASE_URL` /
     `VALHALLA_BASE_URL` in `secrets.properties` / `local.properties`
4. Clear `VALHALLA_API_KEY` / `TRUCKIE_VALHALLA_API_KEY`.
5. Rebuild with `ff_valhalla_live` on — same client as hosted interim.

Bridge/weight packs stay on GitHub Releases. No Firebase.

## Limits + next steps

| Slice | Free Tier | Notes |
| --- | --- | --- |
| SEQ (default) | Yes | Ship this first |
| Wider QLD | Maybe | Widen `--bbox`; watch RAM |
| Full AU | Usually no | Larger VM / regional packs / on-device tiles later |
