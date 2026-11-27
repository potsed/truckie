Feature: Create route with start and end
  As a single-owner-operator driver
  I want to create a route with a verified start and end
  So that I can plan a trip including round trips from the same place

  # Product toggle: ff_route_start_end
  # Navigate sibling: ff_navigate — GraphHopper ALWAYS calculates the constrained path
  # in-app on MapLibre. Preferences/Custom Model shape: low bridges, weight limits,
  # tolls, soft prefer non-residential through-routes; when residential is needed for
  # a delivery, minimise penetration (access only — not a through-route; never
  # hard-block), then Fastest/Cheapest among safe tours. Stub path until live
  # GraphHopper HTTP; full packs remain Phase D. Settings may pick an installed
  # nav app for OPTIONAL post-route handoff only — external apps never replace
  # in-app GraphHopper.
  # Addresses: verified autocomplete only (ff_address_autocomplete / Nominatim AU).
  # Compose Material3 map-home drag sheet only — no global header/footer.
  # Out of scope: intermediate stops beyond start/end, vehicle depth, bridges, tolls, load opt.

  Scenario: Create route start end is feature-flagged
    Given the feature flag ff_route_start_end is off
    Then create route does not require start and end endpoints
    When ff_route_start_end is enabled
    Then create route requires verified start and end addresses with lat/lng

  Scenario: Route domain stores verified start and end
    Given ff_route_start_end is enabled
    When the driver creates a route with a verified start and a verified end
    Then the Route stores formatted address plus lat/lng for start and end
    And the route is stored in the Route domain session

  Scenario: Same start and end is a valid round trip
    Given ff_route_start_end is enabled
    When the driver chooses the same verified place for start and end
    Or selects an explicit round-trip option that copies start to end
    Then the Route is a valid round trip
    And create route still succeeds

  Scenario: Map-home create route sheet collects start and end
    Given ff_map_home and ff_route_start_end are enabled
    And half or full sheet with no active route
    Then Create route shows start and end address fields with autocomplete
    And a round-trip affordance is available
    And Confirm stays disabled until both endpoints are verified
    And Confirm creates the route with start and end

  Scenario: Navigate always calculates GraphHopper path in-app
    Given ff_navigate is enabled
    And an active route with verified start and end
    Then the map-home sheet shows a Navigate action
    When the driver taps Navigate
    Then Truckie always requests a GraphHopper multi-stop tour path in-app
    And the path is drawn on the current MapLibre map by default
    And the same map chrome and drag sheet remain
    And round-trip routes with the same start and end remain valid
    And external navigation apps never replace GraphHopper for path finding
    # See navigate_multi_stop_path.feature (ATNA-87) — not start→end-only.

  Scenario: Settings nav app is optional post-route handoff only
    Given ff_navigate is enabled
    And Settings preference navigation_app_package defaults to Truckie in-app
    When the driver selects an installed navigation app in Settings
    And taps Navigate on a route with start and end
    Then GraphHopper still calculates the constrained path in-app first
    And the path is available on the MapLibre map
    And Truckie may then hand off destination or waypoints from that calculated path
    And the external app is not used as the route calculator

  Scenario: Navigate stays gated without endpoints or when flagged off
    Given ff_navigate is off
    Then the Navigate action is hidden on the map-home sheet
    Given ff_navigate is on and the active route lacks start or end
    Then Navigate is not ready to start
