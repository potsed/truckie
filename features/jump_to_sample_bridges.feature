Feature: Jump to sample bridges (temporary demo control)
  As a single-owner-operator driver dogfooding Truckie
  I want Demo mode without a floating Jump FAB cluttering the map
  So that home chrome stays map-first while sample bridge helpers stay available to tests

  # Product toggle: ff_demo_mode (Settings → Demo mode)
  # Also requires ff_bridge_markers + ff_map_home for markers/surface.
  # ATNA-67: floating Jump control removed from map chrome (dead FAB wiring gone).
  # Camera target helpers remain for unit tests / future non-FAB entry points.
  # Out of scope: pack ingest, GraphHopper routing, Phase C vehicle UI.

  Scenario: Jump floating control is removed from map chrome
    Given ff_map_home and ff_bridge_markers are enabled
    And the feature flag ff_demo_mode is on
    Then the home map does not show Jump to sample bridges
    And Demo mode remains available from Settings

  Scenario: Settings Demo mode switch turns the flag off
    Given ff_demo_mode is enabled
    When the driver turns Demo mode off in Settings
    Then ff_demo_mode is off
    And Jump to sample bridges stays hidden even if ff_bridge_markers is on

  Scenario: Sample bridge camera target stays centred on CBD cluster
    Given ff_demo_mode and ff_bridge_markers are enabled
    And sample bridges sit on real CBD crossings within 2 km of Brisbane CBD fallback
    When a jump-to-sample-bridges camera target is requested
    Then the MapLibre camera target is the SEQ sample cluster near Brisbane CBD
    And the zoom shows several W5-8 markers on actual bridges
    And clearance labels use OpenFreeMap Noto Sans so icons are not hidden by glyph 404s
    And follow mode would exit so location updates do not pull the camera away
