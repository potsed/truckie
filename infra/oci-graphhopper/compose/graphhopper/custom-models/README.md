# Server Custom Models

| File | Role |
| --- | --- |
| `truckie-base.json` | Base model for `profile: truck` (HGV access + soft residential + arterial bias). Derived from GraphHopper 11 built-in `truck.json` access rules, **without** fixed max_height/max_weight hard-blocks — those come per-request from the Android Custom Model. |

## Client wiring

Android `LiveGraphHopperPathRouter` POSTs `/route` with `"profile": "truck"` and a
per-request `custom_model` from `GraphHopperCustomModelStub` (height/weight hard
blocks, soft residential, toll preference hints, `block_under_low_clearance`).

Request-time models **override / merge** with the profile base. CH is disabled in
`config.yml` so Custom Models stay valid.

## Phase D

Bridge / weight restriction packs as encoded edges or avoid `areas` land later.
Until then, pack under-bridges are sent as tiny avoid polygons from the client
(ATNA-102). Keep this directory free of secrets.
