Feature: Retire legacy Activity hosts
  As a single-owner-operator driver
  I want every primary screen hosted in one MainActivity NavHost
  So that map home and chrome never remount through redirect Activities

  # ATNA-74 / P2-1 + ATNA-92 residual cleanup.
  # Product toggles: ff_nav_chrome + ff_map_home still gate chrome / map-home content.
  # Architecture: NavHost-only — delete redirect Activities and dual list-Home Activity path.
  # Out of scope: OCI GraphHopper infra; Phase C product depth; force map-home on in release.

  Scenario: MainActivity is the only screen Activity
    Given Truckie ships Compose Material3 screens
    Then ComposeScreenCatalog lists only MainActivity
    And VehicleListActivity VehicleEditActivity TripStopsActivity AddStopActivity
      BlockedRouteActivity and SettingsActivity are deleted
    And AndroidManifest declares no legacy screen Activities

  Scenario: Chrome host is always the single-activity NavHost
    Given PrimaryNavigation owns primary tabs
    Then shouldUseChromeHost is always true
    And MainActivity always hosts TruckieRootApp
    And ChromeHostRedirect and Activity navigateToPrimaryDestination are deleted
    And TruckieHostedScreen multi-Activity chrome host is deleted

  Scenario: List-first Home only lives inside NavHost when map home is off
    Given ff_map_home is off
    Then Home content is the list-first summary inside MainActivity NavHost
    And there is no parallel MainActivity Activity-host list Home path
    When ff_map_home is enabled
    Then Home uses MapLibre map-first content in the same NavHost

  Scenario: Jump residual and DualMode vehicle wrapper are deleted
    Given Jump FAB was removed in ATNA-67 and DualMode compat ctor in ATNA-59
    Then JumpToSampleBridges helpers and tests are deleted
    And DualModeVehicleProfileRepository leftover wrapper is deleted
    And SafeRouteSelector is deleted — RouteCandidate remains for MultiStopTourComposer
