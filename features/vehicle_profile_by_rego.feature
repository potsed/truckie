Feature: Vehicle profile by registration
  As a driver
  I want vehicle profiles keyed by rego
  So that routing uses the correct dimensions and mass ratings

  @ff_vehicle_profiles
  Scenario: Save a vehicle profile by rego
    Given feature flag "ff_vehicle_profiles" is enabled
    When I save a vehicle with:
      | rego         | ABC-123   |
      | nickname     | Blue rigid|
      | vehicleType  | Rigid     |
      | make         | Volvo     |
      | model        | FM        |
      | year         | 2019      |
      | heightM      | 3.0       |
      | widthM       | 2.5       |
      | lengthM      | 12.5      |
      | massT        | 22.5      |
      | tareT        | 8.5       |
      | gvmT         | 26.0      |
      | gcmT         | 42.5      |
      | axles        | 3         |
      | networkClass | GML       |
    Then the active vehicle profile is "ABC123"
    And the vehicle height is 3.0 m
    And the vehicle GVM is 26.0 t
    And the vehicle make is "Volvo"

  @ff_vehicle_profiles
  Scenario: Select active vehicle from list
    Given feature flag "ff_vehicle_profiles" is enabled
    And vehicles exist for regos "ABC123" and "XYZ987"
    When I select vehicle "XYZ987" as active
    Then the active vehicle profile is "XYZ987"

  @ff_vehicle_profiles
  Scenario: Profiles UI is gated by feature flag
    Given feature flag "ff_vehicle_profiles" is disabled
    Then vehicle profile screens are not available
    And Home does not show an active vehicle summary
