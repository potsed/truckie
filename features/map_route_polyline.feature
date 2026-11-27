Feature: Active route polyline on home map
  As a single-owner-operator driver
  I want the constrained GraphHopper route drawn on MapLibre home
  So that I can see the bridge-safe path between stops before Navigate

  # Product toggle: ff_map_route_polyline (requires ff_map_home).
  # Geometry: PathRouter / MultiStopTourComposer only — never a decorative
  # straight-line between stops. Stub geometry only when the stub PathRouter
  # is the active factory (same as Navigate / Optimize).
  # Constraints (agreed): hard under-passage low bridges (height+buffer) +
  # weight; soft residential; over-deck OK; toll prefs when ff_tolls.
  # Same PathRouter + BridgePassagePolicy + pack bridges as Optimize/Navigate.
  # Rendering: shared MapLibre LineManager path with Navigate
  # (MapRoutePathAnnotations / MapLineBind — not a parallel line stack).
  # Out of scope: satellite peek.

  Scenario: Route polyline is feature-flagged
    Given the feature flag ff_map_route_polyline is off
    Then the home map does not draw an active route polyline
    When ff_map_route_polyline is enabled
    And ff_map_home is enabled
    And the active route has PathRouter-resolvable geometry
    Then the constrained route line is visible on the MapLibre home map

  Scenario: Geometry comes from PathRouter or optimised tour
    Given ff_map_route_polyline is enabled
    And an optimised MultiStopTourComposer path is available
    Then the home map uses that optimised tour geometry
    When no optimised path is cached
    Then the home map composes PathRouter legs in current stop order via ActiveTourPath
    And with ff_route_path_from_location the path starts at current location then start→stops→end
    And geometry is never a DIY straight-line between stop markers
    And Navigate uses that same ActiveTourPath geometry (see navigate_multi_stop_path.feature / route_path_from_location.feature)

  Scenario: Constraints match Optimize and Navigate
    Given ff_map_route_polyline is enabled
    And ff_bridge_passage is enabled with pack bridges
    Then under-passage low bridges hard-block using height plus buffer
    And over-deck passage remains OK
    And weight limits are hard-constrained
    And residential preference is soft only
    And toll preference is applied when ff_tolls is enabled

  Scenario: Fastest and Cost Effective are AND checkboxes
    Given ff_map_route_polyline is enabled
    Then tour selection uses Fastest and Cost Effective as checkboxes
    And both checkboxes are selected by default
    And selection prefers tours that are fast and cost-effective together

  Scenario: Shared LineManager with Navigate
    Given ff_map_route_polyline is enabled
    Then the polyline uses LineManager via MapRoutePathAnnotations
    And managers recreate when style identity drifts or a manager layer is missing
    And satellite peek is not implemented by this feature
