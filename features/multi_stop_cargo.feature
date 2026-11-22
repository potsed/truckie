Feature: Multi-stop trip with cargo on stops
  As a driver
  I want cargo height width length and mass on pick-up and drop-off stops
  So that clearance and weight use load, not only the empty vehicle

  @ff_multi_stop_cargo
  Scenario: Cargo on a pick-up stop increases effective clearance
    Given feature flag "ff_multi_stop_cargo" is enabled
    And the active vehicle height is 3.0 m
    And the system clearance buffer is 0.15 m
    When I add a pick-up stop with cargo height 3.5 m width 2.4 m length 6.0 m mass 8.0 t
    Then the effective clearance height is 3.65 m
