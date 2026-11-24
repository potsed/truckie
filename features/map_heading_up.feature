Feature: Map orients to driving direction while following
  As a single-owner-operator driver
  I want a map control to lock orientation to my driving direction
  So that what is ahead of me stays at the top of the screen while I follow

  # Product toggle: ff_map_heading_up (requires ff_map_home + ff_map_location_puck follow).
  # Map chrome exposes an orientation-lock control next to Recenter (not a Settings switch).
  # While following + locked: MapLibre CameraMode.TRACKING_GPS + RenderMode.GPS.
  # Unlocked: north-up TRACKING. Free pan exits follow; Recenter restores follow
  # (and heading lock when the control is on). Fused Location bearing feeds the puck.

  Scenario: Heading-up is feature-flagged
    Given ff_map_home and ff_map_location_puck are enabled
    And ff_map_heading_up is off
    When follow mode is active
    Then the follow camera mode stays TRACKING (north-up)
    When ff_map_heading_up is enabled
    Then the follow camera mode is TRACKING_GPS
    And the location puck render mode is GPS

  Scenario: Map chrome exposes an orientation lock control
    Given ff_map_home and ff_map_location_puck are enabled
    And the map-home location chrome is shown
    Then an orientation lock control is shown like Recenter
    And the lock and Recenter share the same primary circle visual treatment
    And Settings does not show a Demo-style heading-up switch
    When the driver taps the orientation lock control
    Then ff_map_heading_up toggles on or off
    And while locked and following the camera mode is TRACKING_GPS

  Scenario: Orientation lock and Recenter stay visually consistent
    Given the map-home location chrome is shown
    Then both circular map icon controls use primary surface and onPrimary icon tint
    And only the glyph differs (MyLocation vs Navigation/Explore)
    And unlocked lock state does not use a secondaryContainer treatment

  Scenario: Map orients with GPS bearing while locked
    Given orientation lock is on
    And follow mode is on with a fused location fix that includes bearing
    When the driver is moving
    Then the map camera bearing matches the direction of travel
    And the blue my-location puck points in that direction

  Scenario: Free pan exits heading-up rotation
    Given orientation lock is on and follow mode is on
    When the driver pans the map
    Then follow mode exits
    And the camera mode is NONE
    When the driver taps Recenter
    Then follow mode is re-enabled with TRACKING_GPS heading-up
