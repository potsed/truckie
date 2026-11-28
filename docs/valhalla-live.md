# Live Valhalla (select-user)

Truckie routes **in-app** via [PathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/PathRouter.kt).
Road-following Navigate / Optimise polylines use **Valhalla truck costing**
([LiveValhallaPathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/LiveValhallaPathRouter.kt)).

## Interim vs long-term host

| Phase | Host | Credentials |
| --- | --- | --- |
| **Interim (now)** | Hosted Valhalla — [Simple Routing](https://www.simplerouting.io/hosted-valhalla/) default | `VALHALLA_API_KEY` (Bearer; BuildConfig / secrets — **never commit**) |
| **Long-term / offline** | Self-hosted Valhalla — [`infra/oci-valhalla/`](../infra/oci-valhalla/) (OCI Free Tier) or local compose; on-device tiles later | `VALHALLA_BASE_URL` only; clear API key |

Same Valhalla HTTP `/route` body either way — swap base URL, drop Bearer.

> **Coverage note:** Simple Routing’s free tier documents North America & Europe.
> Australia-first dogfood: deploy **QLD/SEQ** self-hosted tiles via
> `VALHALLA_BASE_URL` ([`oci-valhalla-runbook.md`](./oci-valhalla-runbook.md)).
> The client is host-agnostic.

## Intended select-user path

| Piece | Requirement |
| --- | --- |
| Flag | `ff_valhalla_live` **on** (debug dogfood enables it) |
| Async I/O | `ff_async_routing_io` **on** (defaults on) |
| Interim | Non-blank `VALHALLA_API_KEY` → base defaults to `https://api.simplerouting.io/valhalla` |
| Long-term | Non-blank `VALHALLA_BASE_URL` serving `/route`; clear API key |
| Costing | `costing: truck` + height/width/length/weight from vehicle Custom Model |

Factory: `ValhallaPathRouterFactory.create(liveEnabled, baseUrl, apiKey)`.
App wiring: `TruckieApp.pathRouter()` uses Valhalla live or stub only.

## Configure hosted Valhalla (interim)

Any one of (priority: Gradle property → env → `secrets.properties`):

1. `secrets.properties` (gitignored; copy from `secrets.properties.example`):
   ```
   VALHALLA_API_KEY=your_simplerouting_key_here
   ```
2. Env: `TRUCKIE_VALHALLA_API_KEY=…`
3. Gradle: `-Ptruckie.valhalla.apiKey=…`

```bash
./gradlew :app:assembleDebug \
  -Ptruckie.valhalla.apiKey="$TRUCKIE_VALHALLA_API_KEY"
```

Request: `POST {base}/route` with `Authorization: Bearer …`, `costing: truck`,
and vehicle dimensions in `costing_options.truck`. Shape is polyline **precision 6**.

## Swap to self-hosted / OCI Free Tier (ATNA-9)

1. Deploy [`infra/oci-valhalla/`](../infra/oci-valhalla/) (Terraform + `import-qld-seq.sh`)
   or local compose — see [`oci-valhalla-runbook.md`](./oci-valhalla-runbook.md).
   SEQ (QLD-first) tiles are the Free Tier default; full AU is a next step.
2. Set base URL (any one; priority Gradle → env → `secrets.properties` → `local.properties`):
   - `truckie.valhalla.baseUrl=https://<host>`
   - `TRUCKIE_VALHALLA_BASE_URL=https://<host>`
   - `VALHALLA_BASE_URL=…` in `secrets.properties` or `local.properties`
3. **Clear** `VALHALLA_API_KEY` / `TRUCKIE_VALHALLA_API_KEY`.
4. Rebuild with `ff_valhalla_live` on — same `LiveValhallaPathRouter`.

See `ValhallaLiveCatalog.selfHostedSwapSteps()` and `OciValhallaHostCatalog`.

## Degraded stub

Until credentials resolve, Navigate / map polyline use **stub** PathRouter
(straight / colinear interpolate — **not** road network). Settings and Optimise
summary label this as **degraded stub** (`ValhallaLiveCatalog.routingGeometryStatus`).

## Related

- `features/route_follow_roads.feature`
- `features/oci_valhalla_host.feature`
- `docs/feature-flags.md` (`ff_valhalla_live`)
- `docs/oci-valhalla-runbook.md` (create/apply/destroy + dogfood curl)
- `docs/valhalla-self-host-notes.md`
- `infra/oci-valhalla/`
- `secrets.properties.example`
