# Navigation chrome

Feature flag: `ff_nav_chrome` (on in debug builds).

## Product priority

Audience: **single-owner-operator** drivers who need affordable trip planning.
Core job: **add pickups/dropoffs** and **optimize** for cost-effective, efficient routes.

Bottom nav is **trip-first**: Trip sits right after Home; Settings stays last (secondary stub).

## Destinations

| Bottom tab | Label | Activity | Product flag |
| --- | --- | --- | --- |
| Home | Home | `MainActivity` | — |
| Trip | Trip | `TripStopsActivity` | `ff_multi_stop` |
| Vehicles | Vehicles | `VehicleListActivity` | `ff_vehicle_profiles` |
| Settings | Settings | `SettingsActivity` (stub) | — |

Header titles: Truckie1 · Trip stops · Vehicle profiles · Settings.

Secondary screens (vehicle edit, add stop, blocked route) keep a TopAppBar with back and hide the bottom bar; the selected tab still maps to their parent destination for catalog/tests.

## Implementation

- Shared composable: `TruckieHostedScreen` → `TruckieNavScaffold` (Material3 Scaffold).
- Cross-activity navigation: `navigateToPrimaryDestination` (`CLEAR_TOP` / `SINGLE_TOP`).
- WindowInsets: `TopAppBarDefaults.windowInsets` and `NavigationBarDefaults.windowInsets`.
- High contrast: navy bar containers, white labels/icons (existing Truckie1 palette).

## Address autocomplete (queued)

Stop addresses must be **verified** with **autocomplete** (owner-operator pickups/dropoffs).
This PR only keeps Add Stop chrome ready:

- Hint: “Search address…”
- Hook: `AddStopScreen.onAddressQueryChanged`
- Contract stub: `AddressAutocomplete` / `NoOpAddressAutocomplete` (no provider invented here)
