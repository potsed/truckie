Feature: Compose header and footer navigation
  As a single-owner-operator driver
  I want a persistent Material3 top app bar and trip-first bottom navigation
  So that pickups, dropoffs, and route optimize stay one tap away without expensive nav chrome clutter

  # Product toggle: ff_nav_chrome. Existing product flags still gate Trip / Vehicles entry.
  # Shared Scaffold (not single-activity NavHost) to avoid rewriting domain Activities.
  # Audience: owner-operators — emphasize Trip (pickups/dropoffs + optimize) over Settings.

  Scenario: Primary destinations are trip-first for drivers
    Given nav chrome is catalogued for Truckie1
    Then the primary destinations are Home, Trip, Vehicles, and Settings
    And Trip is ordered immediately after Home before Vehicles and Settings
    And each destination has a high-contrast label for the navy/sky theme

  Scenario: Nav chrome is feature-flagged
    Given the feature flag ff_nav_chrome is off
    Then TopAppBar and bottom NavigationBar are not required
    When ff_nav_chrome is enabled
    Then primary screens use a Material3 TopAppBar title and bottom NavigationBar

  Scenario: Bottom navigation respects product feature flags
    Given ff_nav_chrome is enabled
    When the driver selects Trip
    Then entry still requires ff_multi_stop
    When the driver selects Vehicles
    Then entry still requires ff_vehicle_profiles
    And Settings remains available as a secondary stub

  Scenario: Chrome respects system window insets
    Given edge-to-edge Compose hosting is active
    Then the TopAppBar consumes status bar insets
    And the bottom NavigationBar consumes navigation bar insets
