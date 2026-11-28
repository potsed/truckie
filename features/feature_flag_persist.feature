Feature: Persist feature flags and Demo toggle
  As a single-owner-operator driver
  I want feature flags and Demo mode to survive process death
  So that dogfood toggles and Settings Demo mode stay put after restart

  # ATNA-93 / P2-3. One durable Preferences DataStore path for flag overrides.
  # Production defaults stay off (except catalog DEFAULT_ENABLED structural flags).
  # Debug dogfood seeds fill absent keys only — never overwrite persisted overrides.
  # Demo mode and heading-up persist through LocalFeatureFlags.setEnabled → FeatureFlagStore.
  # No dual write paths (no separate Demo prefs beside the flag store).
  # Out of scope: GraphHopper; G-NAF national pack expansion.

  Scenario: Fresh install keeps production defaults off
    Given a fresh feature-flag store
    When the app binds LocalFeatureFlags from that store
    Then ff_demo_mode is off
    And user-facing flags without catalog DEFAULT_ENABLED stay off

  Scenario: Demo mode toggle survives process death
    Given LocalFeatureFlags bound to a FeatureFlagStore
    When Demo mode is turned on via DemoModeController
    Then ff_demo_mode is on
    And the store contains ff_demo_mode true
    When a new LocalFeatureFlags hydrates from the same store
    Then ff_demo_mode is still on

  Scenario: Turning Demo mode off persists off
    Given Demo mode was previously persisted on
    When Demo mode is turned off via DemoModeController
    And a new LocalFeatureFlags hydrates from the same store
    Then ff_demo_mode is off

  Scenario: Heading-up lock survives process death
    Given LocalFeatureFlags bound to a FeatureFlagStore
    When ff_map_heading_up is enabled via MapHeadingUpCatalog
    And a new LocalFeatureFlags hydrates from the same store
    Then ff_map_heading_up is still on

  Scenario: Debug dogfood seed does not overwrite persisted off
    Given ff_map_heading_up was persisted off
    When the app seeds debug dogfood defaults with seedIfAbsent
    Then ff_map_heading_up stays off

  Scenario: Structural DEFAULT_ENABLED seeds without writing the store
    Given a fresh feature-flag store
    When the app seeds ff_active_tour DEFAULT_ENABLED via seedIfAbsent
    Then ff_active_tour is on in memory
    And the store does not contain ff_active_tour

  Scenario: Single FeatureFlagStore path
    Given feature flags are durable
    Then Demo mode persistence uses the same FeatureFlagStore as other flag overrides
    And there is no separate Demo SharedPreferences write path
