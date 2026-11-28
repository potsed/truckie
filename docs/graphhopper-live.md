# Live GraphHopper (select-user)

Truckie routes **in-app** via [PathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/GraphHopperPathRouter.kt).
Alternates around blocks (low bridges, closures, weight bans — ATNA-102) must use
the **same live PathRouter** (Custom Model / blocked edges), not a hand-rolled
map polyline.

## Intended select-user path

| Piece | Requirement |
| --- | --- |
| Flag | `ff_graphhopper_live` **on** (debug dogfood enables it) |
| Async I/O | `ff_async_routing_io` **on** (defaults on) |
| Base URL | Non-blank `GRAPHHOPPER_BASE_URL` pointing at a GraphHopper `/route` server |
| Graph data | AU / SEQ truck-capable OSM (or equivalent) loaded on that server |
| Custom Model | Vehicle height/weight + `block_under_low_clearance` / soft residential |

Factory: `GraphHopperPathRouterFactory.create(liveEnabled, baseUrl)`.
App wiring: `TruckieApp.pathRouter()` + `GraphHopperLiveCatalog`.

## Configure the base URL

Any one of:

1. Gradle property: `truckie.graphhopper.baseUrl=https://your-gh.example`
2. Env: `TRUCKIE_GRAPHHOPPER_BASE_URL=https://your-gh.example`
3. Written into `BuildConfig.GRAPHHOPPER_BASE_URL` at assemble time

Example:

```bash
./gradlew :app:assembleDebug -Ptruckie.graphhopper.baseUrl=https://gh.example.com
```

### Self-hosted on Oracle Cloud Free Tier

Scaffold: [`infra/oci-graphhopper/`](../infra/oci-graphhopper/) (Terraform + cloud-init + Docker Compose + Caddy).  
Operator steps: [`oci-graphhopper-runbook.md`](./oci-graphhopper-runbook.md).

Point `GRAPHHOPPER_BASE_URL` at `https://<your-domain>` (Caddy proxies `/route` to GraphHopper). Do **not** use GraphHopper Cloud. Daniel supplies OCI Free Tier credentials — nothing is committed.

## What is missing when live is not active

`GraphHopperLiveCatalog.missingForLiveRouting(flags)` reports gaps, typically:

- **`GRAPHHOPPER_BASE_URL` unset** — most common; factory falls back to stub
- **`ff_graphhopper_live` off** — production default until URL + AU data confirmed
- **`ff_async_routing_io` off** — live HTTP is not allowed on the Compose path

Until the URL + AU graph are configured, Navigate / map polyline still run on the
**stub** PathRouter (drawable legs for dogfood UI). Stub may simulate avoid-list
detours in unit tests only — it is **not** the production alternate design.

## Block alternates (ATNA-102)

1. Compose PathRouter leg with truck Custom Model.
2. If Blocked or under-passage pack conflict → `PathBlockDetour.resolveLeg` retries
   PathRouter with avoid bridges (live: Custom Model areas + blocked edges).
3. Prefer any PathRouter `Ok` (≥2 points) over erasing the LineManager line.
4. Cargo GVM / missing coordinates remain non-geographic `Blocked` (no inventing a tour).

## Related

- `features/path_block_detour.feature`
- `features/route_optimisation_graphhopper.feature`
- `features/oci_graphhopper_host.feature`
- `docs/feature-flags.md` (`ff_graphhopper_live`, `ff_path_block_detour`)
- `docs/oci-graphhopper-runbook.md`
- `infra/oci-graphhopper/README.md`
