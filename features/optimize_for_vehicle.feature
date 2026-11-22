Feature: Optimize for this vehicle
  As a driver
  I want Fastest or Cheapest among height- and weight-safe routes
  So that I save time or money without sacrificing clearance safety

  @ff_route_optimize
  Scenario: Choose fastest among safe routes
    Given feature flag "ff_route_optimize" is enabled
    And multiple height- and weight-safe routes exist
    When I optimize for "Fastest"
    Then the selected route is the shortest duration safe option

  @ff_route_optimize
  Scenario: Choose cheapest among safe routes
    Given feature flag "ff_route_optimize" is enabled
    And multiple height- and weight-safe routes exist
    When I optimize for "Cheapest"
    Then the selected route is the lowest cost safe option

  @ff_route_optimize @ff_multi_stop @ff_stop_cargo
  Scenario: Persist optimized stop order
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_multi_stop" is enabled
    And an active vehicle with on-board cargo clearance and mass constraints
    When I optimize for "Fastest" from trip stops
    Then the trip stop order is replaced with the optimized tour order

  @ff_route_optimize
  Scenario: Blocked when no feasible tour
    Given feature flag "ff_route_optimize" is enabled
    And no height- and weight-safe tour exists for the active vehicle
    When I optimize for "Fastest"
    Then the result is a blocked route with a clearance or mass reason

  @ff_route_optimize
  Scenario: Optimize UI is gated by flag
    Given feature flag "ff_route_optimize" is disabled
    Then "Optimize for this vehicle" is not available
