Feature: Demo mode
  As a driver dogfooding Truckie
  I want a Settings toggle for Demo mode
  So that temporary demo affordances can be shown or hidden

  # Product toggle: ff_demo_mode
  # Demo mode is off by default in production AND debug — opt-in via Settings only.

  Scenario: Demo mode defaults off
    Given a fresh app start
    Then ff_demo_mode is off
    And Demo mode is disabled even in debug dogfood

  Scenario: Demo mode is feature-flagged
    Given the feature flag ff_demo_mode is off
    Then Demo mode is disabled
    When ff_demo_mode is enabled
    Then Demo mode is enabled

  Scenario: Settings exposes a Demo mode switch
    Given the Settings screen is open
    Then a clear Demo mode switch is shown
    When the switch is turned off
    Then ff_demo_mode is off
    When the switch is turned on
    Then ff_demo_mode is on
