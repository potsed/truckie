Feature: Filter home-map bridges by vehicle height
  As a single-owner-operator driver
  I want the home map to show only low bridges my active vehicle cannot clear
  So that W5-8 symbols highlight real conflicts, not every low structure in the pack

  # Product toggle: ff_vehicle_height_bridge_filter (also needs ff_bridge_markers)
  # Conflict rule: published clearance < (vehicleHeight + system buffer)
  # Buffer: ClearanceBuffer (Settings, default 0.15 m, clamped 0.15–1.0 m)
  # Keeps W5-8 low-only rules (ff_w5_8_low_only); never invents heights for unknown/N/A
  # Fallback when no active vehicle / no usable height: show all low-pack markers (demo/Jump)
  # ATNA-47

  Scenario: Vehicle-height bridge filter is feature-flagged
    Given the feature flag ff_vehicle_height_bridge_filter is off
    Then the vehicle-height conflict filter is not active
    When ff_vehicle_height_bridge_filter is enabled
    And ff_bridge_markers is enabled
    And an active vehicle has a usable height
    Then only conflict bridges (clearance below height + buffer) are shown

  Scenario: Active vehicle with usable height shows conflicts only
    Given ff_bridge_markers, ff_w5_8_low_only, and ff_vehicle_height_bridge_filter are enabled
    And the Australia-first low-bridge pack is loaded
    And the active vehicle height is 4.0 m with the default system buffer 0.15 m
    Then the conflict threshold is 4.15 m
    And Story Bridge (3.8 m) is shown as a conflict
    And a verified low bridge at 4.3 m is not shown (vehicle can clear it)
    And unknown / not_applicable spans still do not receive W5-8 or invented heights

  Scenario: No active vehicle or no usable height falls back to all low markers
    Given ff_vehicle_height_bridge_filter is enabled
    And ff_w5_8_low_only is enabled
    When there is no active vehicle
    Or the active vehicle has no usable height
    Then all W5-8 low-pack markers are shown (same as today)
    And the Brisbane CBD sample cluster still has low markers for dogfood

  Scenario: System clearance buffer is used for the conflict threshold
    Given ff_vehicle_height_bridge_filter is enabled
    And the system buffer is the Settings ClearanceBuffer (default 0.15 m)
    When the active vehicle height is 4.0 m and buffer is 0.3 m
    Then a bridge at 4.2 m conflicts (4.2 < 4.3)
    And a bridge at 4.3 m does not conflict
