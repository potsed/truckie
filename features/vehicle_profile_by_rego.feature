Feature: Vehicle profile by registration
  As a driver
  I want vehicle profiles keyed by rego
  So that routing uses the correct dimensions and mass

  @ff_vehicle_profiles
  Scenario: Save a vehicle profile by rego
    Given feature flag "ff_vehicle_profiles" is enabled
    When I save a vehicle with rego "ABC-123" height 3.0 m width 2.5 m length 12.5 m mass 22.5 t
    Then the active vehicle profile is "ABC-123"
    And the vehicle height is 3.0 m
