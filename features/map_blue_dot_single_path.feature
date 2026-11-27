Feature: Single blue-dot / follow path (no competing LocationComponent updates)
  As a single-owner-operator driver
  I want one clear follow / puck / Recenter / heading-up policy
  So the blue location dot stays centred and does not fight Recenter or orientation

  # ATNA-69 — refactor over patch. Competing SideEffect + GPS + AndroidView
  # TRACKING enter paths, dual forceLocationUpdate, and heading-up that never
  # re-applies TRACKING↔TRACKING_GPS mid-follow left the puck broken after #46.
  # Gated by existing ff_map_home + ff_map_location_puck (+ ff_map_heading_up
  # for orientation). No new feature flag.

  Scenario: One reconcile path owns puck push and follow enter
    Given ff_map_home and ff_map_location_puck are enabled
    And follow mode is on with a fused location fix
    When a location fix arrives while LocationComponent is not yet tracking
    Then the single reconcile path pushes the puck once
    And enters follow via a bare TRACKING or TRACKING_GPS set
    And does not schedule a separate GPS moveCamera

  Scenario: Steady motion relies on LocationComponent only
    Given follow mode is on and LocationComponent is already tracking
    When further fused location fixes arrive
    Then the reconcile path pushes the puck only
    And does not re-enter TRACKING or drive camera itself

  Scenario: Orientation lock syncs modes while already following
    Given follow / center lock is on with north-up TRACKING
    When the driver engages heading-up orientation lock
    Then the single path syncs to TRACKING_GPS and GPS render mode
    Without exiting follow or starting Recenter
    And Recenter zoom / single animate path stays unchanged

  Scenario: Stuck recenter arm cannot permanently block follow
    Given a Recenter token fired but Recenter could not run (no fix / flags off)
    When the host reconciles RecenterUnavailable
    Then the stuck recenter-in-flight arm is cleared
    And later compose sync can enter TRACKING so the puck stays centred

  Scenario: Recenter remains one animate then bare follow enter
    Given the driver has panned away from follow
    When the driver taps Recenter
    Then one animateCamera frames lat/lng at default closer zoom
    And follow enters via bare camera-mode after syncing last location
    And no parallel SideEffect arm leaves ignoreTrackingDismiss stuck
