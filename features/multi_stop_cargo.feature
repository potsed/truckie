Feature: Multi-stop trip with cargo on stops
  As a driver
  I want ordered stops with pick-up and drop-off cargo
  So that clearance uses on-board load height, not only the empty vehicle

  @ff_multi_stop
  Scenario: Driver can reorder add and delete trip stops
    Given feature flag "ff_multi_stop" is enabled
    When I add stops "Depot" "Site A" "Yard"
    And I move stop "Yard" before "Site A"
    And I delete stop "Site A"
    Then the trip stop order is "Depot" "Yard"

  @ff_multi_stop @ff_stop_cargo
  Scenario: Cargo on a pick-up stop increases effective clearance
    Given feature flag "ff_multi_stop" is enabled
    And feature flag "ff_stop_cargo" is enabled
    And the active vehicle height is 3.0 m
    And the system clearance buffer is 0.15 m
    When I add a pick-up stop with cargo height 3.5 m width 2.4 m length 6.0 m mass 8.0 t
    Then the effective clearance height after that stop is 3.65 m

  @ff_multi_stop @ff_stop_cargo
  Scenario: Drop-off removes cargo from on-board clearance
    Given feature flag "ff_multi_stop" is enabled
    And feature flag "ff_stop_cargo" is enabled
    And the active vehicle height is 3.0 m
    And the system clearance buffer is 0.15 m
    When I add a pick-up stop with cargo description "crates" height 3.5 m
    And I add a drop-off stop with cargo description "crates" height 3.5 m
    Then the effective clearance height after the drop-off is 3.15 m
