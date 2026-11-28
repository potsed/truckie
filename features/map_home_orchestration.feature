Feature: Map home orchestration split
  As a single-owner-operator driver
  I want map-home, RootNav, and MapLibre wiring split into controllers and a thin host
  So that Phase C changes stay readable, low-complexity, and free of god-file merge conflicts

  # Architecture (P1-5 / ATNA-90): refactor only — no product behaviour change.
  # Gated by existing ff_map_home (and related map flags). No dual orchestration path.
  # Out of scope: delete legacy Activities (ATNA-74).

  Scenario: Map-home domain lives in MapHomeController
    Given ff_map_home is enabled
    Then map-home create-route, add-stop, Navigate, sheet value, demo rebind, and bridges
      are owned by MapHomeController
    And TruckieRootNav MapHomeRoute is a thin Compose binder only
    And there is no inline god MapHomeRoute domain body left in TruckieRootNav

  Scenario: MapLibreHost is a thin command executor
    Given MapLibre map home is shown
    Then MapLibreHost lives in its own file
    And follow, puck, Recenter, and heading-up still go through MapFollowReconcile
      then MapLibreHost.executeFollowCommands
    And bind applies a MapLibreBindSnapshot (stops, path, pre-labelled bridges)
    And MapLibreHost does not look up TruckieApp feature flags during bind

  Scenario: Route sheet presentation is modular
    Given ff_map_home is enabled
    Then RouteSheetBody, CreateRouteForm, AddFindStopsSection, and StopListSection
      live outside the MapHomeScreen layout host file
    And MapHomeScreen remains the layout host for map + sheet + chrome FABs

  Scenario: RootNav destinations are split files
    Given the single-activity NavHost shell
    Then TripStops, Vehicle list/edit, and AddStop route controllers live in dedicated files
    And TruckieRootApp keeps Scaffold + NavHost wiring only
    And legacy Activities are not deleted in this change
