# Navigation chrome

Feature flag: `ff_nav_chrome` (on in debug builds).

## Product priority

Audience: **single-owner-operator** drivers who need affordable trip planning.
Core job: **add pickups/dropoffs** and **optimize** for cost-effective, efficient routes.

Bottom nav is **trip-first**: Trip sits right after Home; Settings stays last (secondary stub).

## Global fixed chrome

**Compose only** — Material3 `Scaffold` / `CenterAlignedTopAppBar` / `NavigationBar` +
Compose `NavHost`. No XML layouts, AppCompat ActionBar, or view-based chrome.

**NavHost-only (ATNA-74):** `MainActivity` is the sole screen Activity and always hosts
`TruckieRootApp`. Legacy redirect Activities (`TripStopsActivity`, `VehicleListActivity`,
`SettingsActivity`, …), `ChromeHostRedirect`, and Activity
`navigateToPrimaryDestination` are deleted.

- One Material3 `Scaffold` at the root
- Compose **NavHost** in the content slot (Home / Trip / Vehicles / Settings + secondary routes)
- Bars **never remount** when switching primary tabs
- Only destination content scrolls in the middle
- WindowInsets: `TopAppBarDefaults.windowInsets` + `NavigationBarDefaults.windowInsets`
- **One path:** `PrimaryNavigation` owns primary-tab NavOptions (Settings → Home
  remounts map home fresh — no `restoreState` — so MapLibre is not left dead under
  hidden chrome)
- `ff_nav_chrome` / `ff_map_home` still gate chrome bars and map-home content **inside**
  the NavHost; they no longer switch between Activity hosts

See also: `features/settings_home_nav.feature`, `features/legacy_activity_retirement.feature`.

## Destinations

| Bottom tab | Label | Nav route | Product flag |
| --- | --- | --- | --- |
| Home | Home | `home` | — |
| Trip | Trip | `plan_trip` | `ff_multi_stop` |
| Vehicles | Vehicles | `vehicles` | `ff_vehicle_profiles` |
| Settings | Settings | `settings` | — |

Secondary routes (no bottom bar; TopAppBar back): `add_stop`, `vehicle_edit`, `blocked_route/...`.

Header titles: Truckie · Trip stops · Vehicle profiles · Settings.

When `ff_map_home` is off, Home is the list-first summary **inside** the same NavHost
(not a parallel Activity host).

## Address autocomplete (queued)

Stop addresses must be **verified** with **autocomplete** (owner-operator pickups/dropoffs).
Hooks remain ready:

- Hint: “Search address…”
- Hook: `AddStopScreen.onAddressQueryChanged`
- Contract stub: `AddressAutocomplete` / `NoOpAddressAutocomplete` (no provider invented here)
