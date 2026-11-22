Feature: Bridge-safe route
  As a driver
  I want routes that respect effective clearance and mass
  So that I avoid low bridges and weight-restricted structures

  @ff_bridge_safe_routing
  Scenario: Route blocked when bridge is below effective clearance
    Given feature flag "ff_bridge_safe_routing" is enabled
    And the effective clearance height is 3.65 m
    When GraphHopper returns a candidate with max height 3.4 m
    Then the route is rejected as unsafe
