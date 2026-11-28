Feature: Route path from current location through tour
  As a single-owner-operator driver
  I want the map polyline and Navigate path to start at my current location
  So that I can see how to reach the route start, then follow stop order to the end

  # Product toggle: ff_route_path_from_location (defaults on).
  # Requires ff_map_route_polyline (preview) and/or ff_navigate + ff_navigate_multi_stop.
  # Geometry: ActiveTourPath ordered PathRouter legs —
  #   current location → route start → ordered stops → route end.
  # When current location is unavailable, fall back to start → stops → end.
  # Optimise still permutes start-fixed tours (current is approach-only, not origin).
  # Shared LineManager bind with Navigate (MapRoutePathAnnotations / MapLineBind).
  # Out of scope: async PathRouter (P1-3), Room persist (P1-4), god-file splits (P1-5).

  Scenario: Approach-from-location is feature-flagged and defaults on
    Given a fresh app start
    Then ff_route_path_from_location is on
    And ActiveTourPath prepends current location before route start when a fix exists

  Scenario: Polyline and Navigate include current then start then stops then end
    Given ff_route_path_from_location, ff_map_route_polyline, and ff_navigate_multi_stop are enabled
    And an active route with verified start, ordered stops, and end
    And an active vehicle
    And a fused current-location fix distinct from route start
    When the home polyline resolves or the driver taps Navigate
    Then the path starts at the current location
    And the path visits route start, each ordered stop, then route end
    And geometry is PathRouter / MultiStopTourComposer legs (never DIY straight-lines)

  Scenario: Without a location fix the path still shows start through end
    Given ff_route_path_from_location is enabled
    And an active route with endpoints and vehicle
    And no current-location fix
    When the home polyline resolves
    Then the path starts at route start through ordered stops to route end

  Scenario: How to see the path on the map
    Given ff_map_home and ff_map_route_polyline are on (debug dogfood defaults)
    And an active vehicle is selected
    And the active route has verified start and end
    Then MapLibre draws the constrained line via shared LineManager
    And without location permission the approach leg is omitted but start→stops→end remains
