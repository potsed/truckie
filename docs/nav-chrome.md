# Navigation chrome

Feature flag: `ff_nav_chrome` (on in debug builds).

## Product priority

Audience: **single-owner-operator** drivers who need affordable trip planning.
Core job: **add pickups/dropoffs** and **optimize** for cost-effective, efficient routes.

Bottom nav is **trip-first**: Trip sits right after Home; Settings stays last (secondary stub).

## Global fixed chrome

**Compose only** — Material3 `Scaffold` / `CenterAlignedTopAppBar` / `NavigationBar` +
Compose `NavHost`. No XML layouts, AppCompat ActionBar, or view-based chrome.

With `ff_nav_chrome` **or** `ff_map_home` enabled, header + footer are a **global
fixed** shell on **`MainActivity`** (map home forces the same single-activity host):

- One Material3 `Scaffold` at the root
- Compose **NavHost** in the content slot (Home / Trip / Vehicles / Settings + secondary routes)
- Bars **never remount** when switching primary tabs
- Only destination content scrolls in the middle
- WindowInsets: `TopAppBarDefaults.windowInsets` + `NavigationBarDefaults.windowInsets`
- **One path:** `PrimaryNavigation` owns primary-tab NavOptions (Settings → Home
  remounts map home fresh — no `restoreState` — so MapLibre is not left dead under
  hidden chrome)

Legacy Activities (`TripStopsActivity`, `VehicleListActivity`, …) redirect into
`MainActivity` with `EXTRA_ROUTE` when the chrome host gate is on. When both flags
are off, those Activities remain the multi-Activity path without chrome.

See also: `features/settings_home_nav.feature`.

## Destinations

| Bottom tab | Label | Nav route | Product flag |
| --- | --- | --- | --- |
| Home | Home | `home` | — |
| Trip | Trip | `plan_trip` | `ff_multi_stop` |
| Vehicles | Vehicles | `vehicles` | `ff_vehicle_profiles` |
| Settings | Settings | `settings` | — |

Secondary routes (no bottom bar; TopAppBar back): `add_stop`, `vehicle_edit`, `blocked_route/...`.

Header titles: Truckie · Trip stops · Vehicle profiles · Settings.

## Address autocomplete (queued)

Stop addresses must be **verified** with **autocomplete** (owner-operator pickups/dropoffs).
Hooks remain ready:

- Hint: “Search address…”
- Hook: `AddStopScreen.onAddressQueryChanged`
- Contract stub: `AddressAutocomplete` / `NoOpAddressAutocomplete` (no provider invented here)
