Feature: Map orients to driving direction while following
  As a single-owner-operator driver
  I want a Settings switch for map heading-up
  So that I can choose whether the map rotates to match the direction I am driving

  # Product toggle: ff_map_heading_up (requires ff_map_home + ff_map_location_puck follow).
  # Settings exposes a switch that flips the same flag (same pattern as Demo mode).
  # While following + on: MapLibre CameraMode.TRACKING_GPS + RenderMode.GPS.
  # When the driver pans away (free mode): north-up / NONE — no forced rotation.
  # Recenter restores heading-up follow when the switch is on.
  # Fused Location bearing feeds the puck.

  Scenario: Heading-up is feature-flagged
    Given ff_map_home and ff_map_location_puck are enabled
    And ff_map_heading_up is off
    When follow mode is active
    Then the follow camera mode stays TRACKING (north-up)
    When ff_map_heading_up is enabled
    Then the follow camera mode is TRACKING_GPS
    And the location puck render mode is GPS

  Scenario: Settings exposes a heading-up switch
    Given the Settings screen is open
    Then a clear Orient map to driving direction switch is shown
    When the switch is turned on
    Then ff_map_heading_up is on
    When the switch is turned off
    Then ff_map_heading_up is off
    And follow camera mode stays TRACKING (north-up)

  Scenario: Map orients with GPS bearing while following
    Given ff_map_heading_up is enabled via the Settings switch
    And follow mode is on with a fused location fix that includes bearing
    When the driver is moving
    Then the map camera bearing matches the direction of travel
    And the blue my-location puck points in that direction

  Scenario: Free pan exits heading-up rotation
    Given ff_map_heading_up is enabled and follow mode is on
    When the driver pans the map
    Then follow mode exits
    And the camera mode is NONE
    When the driver taps Recenter
    Then follow mode is re-enabled with TRACKING_GPS heading-up
