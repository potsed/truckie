Feature: Center lock keeps the location puck map-centred while moving
  As a single-owner-operator driver
  I want follow / center lock to keep my blue location dot at map centre while I drive
  So that the map moves under me instead of the puck drifting off-centre

  # Gated by existing ff_map_home + ff_map_location_puck (no new flag).
  # Steady follow: LocationComponent TRACKING / TRACKING_GPS owns the camera via
  # forceLocationUpdate — do not GPS moveCamera (keeps Recenter single-path).
  # Follow enter after Recenter must not use setCameraMode(mode, transitionMs=0, …):
  # MapLibre LocationCameraController can leave isTransitioning stuck, so later
  # location updates move the puck but not the camera (ATNA-66).

  Scenario: Puck stays centred while following and the device moves
    Given ff_map_home and ff_map_location_puck are enabled
    And follow mode is on with a fused location fix
    When the device location updates while still following
    Then LocationComponent camera mode stays TRACKING or TRACKING_GPS
    And the blue my-location puck remains locked at map centre
    And the host does not apply a separate GPS moveCamera or zoom

  Scenario: Manual pan breaks center lock and Recenter restores it
    Given follow mode is on with the puck centred
    When the driver pans the map
    Then follow mode exits and Recenter is shown
    When the driver taps Recenter
    Then one animateCamera frames lat/lng at default closer zoom
    And follow enters via a bare camera-mode set after syncing last location
    And follow enter does not use a zero-duration timed setCameraMode overload
    And the puck is centred again with TRACKING or TRACKING_GPS as appropriate

  Scenario: Heading-up lock stays independent of center lock
    Given ff_map_heading_up is on and orientation lock is engaged
    And follow / center lock is on
    When the device location updates
    Then follow camera mode is TRACKING_GPS
    And Recenter zoom / single-path behaviour is unchanged
