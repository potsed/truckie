Feature: Optimize for this vehicle
  As a driver
  I want Fastest or Cheapest among height- and weight-safe routes
  So that I save time or money without sacrificing clearance safety

  @ff_optimize_for_vehicle
  Scenario: Choose fastest among safe routes
    Given feature flag "ff_optimize_for_vehicle" is enabled
    And multiple height- and weight-safe routes exist
    When I optimize for "Fastest"
    Then the selected route is the shortest duration safe option
