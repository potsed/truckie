Feature: Geographic tour stop order under constraints
  As a single-owner-operator driver
  I want Optimise to choose a geographic tour-like stop order
  So that I do closer stops first and avoid city zig-zag when a tighter tour exists

  # ATNA-120. Preferences: logical order, closer stops first, shortest feasible
  # within bridge/weight/toll constraints. PathRouter (Valhalla) legs score tours.
  # Start and end stay anchored; only intermediate stops are reordered.

  @ff_route_optimize @ff_geographic_tour_order
  Scenario: SEQ-like zig-zag input does not pick farthest stop first
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_geographic_tour_order" is enabled
    And a round-trip tour start north of Brisbane with intermediate stops in zig-zag order
    And the farthest stop is listed immediately after start in the raw input
    When I optimise with PathRouter tour composition
    Then start and end remain anchored
    And the first intermediate stop is not the farthest stop
    And the chosen tour is shorter than the raw zig-zag order

  @ff_route_optimize @ff_geographic_tour_order
  Scenario: Destination stays fixed while middle stops permute
    Given feature flag "ff_geographic_tour_order" is enabled
    And a tour with verified start, intermediate stops, and end
    When MultiStopTourComposer builds candidate orders
    Then every candidate keeps the same start first and the same end last
    And only intermediate stops are permuted

  @ff_route_optimize @ff_geographic_tour_order @ff_active_tour
  Scenario: Optimise uses ActiveTourPath waypoints then writes intermediate order
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_geographic_tour_order" is enabled
    And an active route with start, end, and multi-stop waypoints
    When I run Optimise for the active vehicle
    Then PathRouter scores the start-anchored geographic tour
    And the persisted stop list is the intermediate order without synthetic start/end ids

  @ff_geographic_tour_order
  Scenario: Geographic tour order is feature-flagged
    Given feature flag "ff_geographic_tour_order" is disabled
    Then MultiStopTourComposer keeps legacy origin-only candidate orders
    And when the flag is enabled destination-anchored geographic orders are used
