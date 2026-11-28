Feature: Jump to sample bridges residual deleted
  As a single-owner-operator driver dogfooding Truckie
  I want Demo mode without Jump residual helpers cluttering the codebase
  So that map chrome stays map-first and dead affordances do not confuse agents

  # Product toggle: ff_demo_mode (Settings → Demo mode)
  # ATNA-67: floating Jump FAB removed. ATNA-92: JumpToSampleBridges residual deleted.
  # Out of scope: pack ingest, GraphHopper routing, Phase C vehicle UI.

  Scenario: Jump residual is deleted from map chrome and helpers
    Given ff_map_home and ff_bridge_markers are enabled
    And the feature flag ff_demo_mode is on
    Then the home map does not show Jump to sample bridges
    And JumpToSampleBridges helpers are deleted
    And Demo mode remains available from Settings

  Scenario: Settings Demo mode switch turns the flag off
    Given ff_demo_mode is enabled
    When the driver turns Demo mode off in Settings
    Then ff_demo_mode is off
    And Jump to sample bridges stays absent even if ff_bridge_markers is on
