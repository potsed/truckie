Feature: Demo mode
  As a driver dogfooding Truckie
  I want a Settings toggle for Demo mode with a sample SEQ run
  So that I can try route optimisation without mixing in my real vehicles or routes

  # Product toggle: ff_demo_mode
  # Demo mode is off by default in production AND debug — opt-in via Settings only.
  # When on: only the demo vehicle + demo route/stops are visible.
  # When off: only real (non-demo) vehicles + routes are visible — hard isolation.

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

  Scenario: Demo dataset has depot, six stops, and a 3.5 m vehicle
    Given Demo mode is enabled
    Then the active vehicle is the demo vehicle at 3.5 m height
    And the active route depot is DHL at 1A Boronia Road Brisbane Airport
    And the route has 2 pickup/load stops and 4 drop-off/unload stops
    And five stops are in industrial areas (Yatala, Lytton, or similar)
    And exactly one stop is in a residential area
    And at least one stop is near a verified low bridge (W5-8 / clearance dogfood)

  Scenario: Demo and real data are hard-isolated
    Given real vehicles and a real active route exist
    When Demo mode is turned on
    Then real vehicles are not listed
    And the real route is not the active route
    And only the demo vehicle and demo route are available
    When Demo mode is turned off
    Then the demo vehicle is not listed
    And the demo route is not the active route
    And real vehicles and the real route are available again
