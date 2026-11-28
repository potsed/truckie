# Valhalla self-host / OCI notes

Truckie’s **sole** live PathRouter is Valhalla (`ff_valhalla_live` /
[LiveValhallaPathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/LiveValhallaPathRouter.kt)).

This document is **docs-only** guidance for a future self-hosted or on-device
Valhalla tile host. It does **not** ship Terraform, Compose, or an apply runbook —
the former GraphHopper OCI scaffold (`infra/oci-graphhopper/`) was removed with
ATNA-114.

## App wiring (already in-tree)

| Piece | Where |
| --- | --- |
| Flag | `ff_valhalla_live` |
| Catalog | `ValhallaLiveCatalog` / `ValhallaLiveCatalog.selfHostedSwapSteps()` |
| Factory | `ValhallaPathRouterFactory` |
| Interim hosted | `VALHALLA_API_KEY` → Simple Routing default base |
| Self-host / offline | `VALHALLA_BASE_URL` serving `/route`; **clear** API key |

See [`valhalla-live.md`](./valhalla-live.md) for credentials and request shape.

## Operator intent (when tiles exist)

1. Build or obtain **AU** Valhalla tiles (OCI Free Tier VM or on-device pack).
2. Expose Valhalla’s HTTP `/route` behind HTTPS (Caddy/nginx — your choice).
3. Point Truckie at the host:
   - `truckie.valhalla.baseUrl=https://<domain>` or `TRUCKIE_VALHALLA_BASE_URL` /
     `VALHALLA_BASE_URL` in `secrets.properties`
4. Clear `VALHALLA_API_KEY` / `TRUCKIE_VALHALLA_API_KEY`.
5. Rebuild with `ff_valhalla_live` on — same client as hosted interim.

Do **not** invent or commit OCI Terraform here until a Valhalla self-host task
owns infra. Bridge/weight packs stay on GitHub Releases. No Firebase.
