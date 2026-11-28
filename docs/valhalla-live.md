# Live Valhalla (select-user)

Truckie routes **in-app** via [PathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/GraphHopperPathRouter.kt).
Road-following Navigate / Optimise polylines use **Valhalla truck costing**
([LiveValhallaPathRouter](../core-routing/src/main/kotlin/com/truckie1/routing/LiveValhallaPathRouter.kt)).

GraphHopper Cloud was abandoned (signup closed). Legacy `ff_graphhopper_live` remains
a secondary fallback only when Valhalla credentials are missing.

## Interim vs long-term host

| Phase | Host | Credentials |
| --- | --- | --- |
| **Interim (now)** | Hosted Valhalla — [Simple Routing](https://www.simplerouting.io/hosted-valhalla/) default | `VALHALLA_API_KEY` (Bearer; BuildConfig / secrets — **never commit**) |
| **Long-term / offline** | Self-hosted or on-device Valhalla tiles (OCI Free Tier OK) | `VALHALLA_BASE_URL` only; clear API key |

Same Valhalla HTTP `/route` body either way — swap base URL, drop Bearer.

> **Coverage note:** Simple Routing’s free tier documents North America & Europe.
> Australia-first dogfood may need a paid / bespoke hosted plan **or** self-hosted
> AU tiles via `VALHALLA_BASE_URL`. The client is host-agnostic.

## Intended select-user path

| Piece | Requirement |
| --- | --- |
| Flag | `ff_valhalla_live` **on** (debug dogfood enables it) |
| Async I/O | `ff_async_routing_io` **on** (defaults on) |
| Interim | Non-blank `VALHALLA_API_KEY` → base defaults to `https://api.simplerouting.io/valhalla` |
| Long-term | Non-blank `VALHALLA_BASE_URL` serving `/route`; clear API key |
| Costing | `costing: truck` + height/width/length/weight from vehicle Custom Model |

Factory: `ValhallaPathRouterFactory.create(liveEnabled, baseUrl, apiKey)`.
App wiring: `TruckieApp.pathRouter()` prefers Valhalla over GraphHopper live.

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

## Swap to self-hosted / offline tiles (later)

1. Build or obtain **AU** Valhalla tiles (OCI Free Tier or on-device pack).
2. Set base URL (any one):
   - `truckie.valhalla.baseUrl=https://<host>`
   - `TRUCKIE_VALHALLA_BASE_URL=https://<host>`
   - `VALHALLA_BASE_URL=…` in `secrets.properties`
3. **Clear** `VALHALLA_API_KEY` / `TRUCKIE_VALHALLA_API_KEY`.
4. Rebuild with `ff_valhalla_live` on — same `LiveValhallaPathRouter`.

See `ValhallaLiveCatalog.selfHostedSwapSteps()`.

## Degraded stub

Until credentials resolve, Navigate / map polyline use **stub** PathRouter
(straight / colinear interpolate — **not** road network). Settings and Optimise
summary label this as **degraded stub** (`ValhallaLiveCatalog.routingGeometryStatus`).

## Related

- `features/route_follow_roads.feature`
- `docs/feature-flags.md` (`ff_valhalla_live`)
- `docs/graphhopper-live.md` (legacy GH live / OCI GH scaffold)
- `secrets.properties.example`
