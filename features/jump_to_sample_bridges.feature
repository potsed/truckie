Feature: Jump to sample bridges (temporary demo control)
  As a single-owner-operator driver dogfooding Truckie
  I want a temporary map control to fly to the SEQ sample bridge cluster
  So that I can see W5-8 markers when my GPS is elsewhere

  # Product toggle: ff_demo_mode (Settings → Demo mode)
  # Also requires ff_bridge_markers + ff_map_home for markers/surface.
  # TEMPORARY / DEV affordance — remove when the full AU pack (or a better
  # discoverability path) lands. Do not expand the full AU dataset here.
  # Map chrome only (no global header/footer). Out of scope: pack ingest,
  # GraphHopper routing, Phase C vehicle UI.

  Scenario: Jump control is gated by demo mode
    Given ff_map_home and ff_bridge_markers are enabled
    And the feature flag ff_demo_mode is off
    Then the home map does not show Jump to sample bridges
    When Demo mode is turned on in Settings
    Then Jump to sample bridges is visible on the MapLibre home map chrome

  Scenario: Settings Demo mode switch turns the flag off
    Given ff_demo_mode is enabled
    When the driver turns Demo mode off in Settings
    Then ff_demo_mode is off
    And Jump to sample bridges is hidden even if ff_bridge_markers is on

  Scenario: Tap flies camera to Brisbane CBD sample cluster
    Given ff_demo_mode and ff_bridge_markers are enabled
    And sample bridges sit within 2 km of Brisbane CBD fallback
    When the driver taps Jump to sample bridges
    Then the MapLibre camera animates to the SEQ sample cluster near Brisbane CBD
    And the zoom shows several W5-8 markers
    And clearance labels use OpenFreeMap Noto Sans so icons are not hidden by glyph 404s
    And follow mode exits so location updates do not pull the camera away
