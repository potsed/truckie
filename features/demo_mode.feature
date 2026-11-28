Feature: Demo mode
  As a driver dogfooding Truckie
  I want a Settings toggle for Demo mode with a sample SEQ run
  So that I can try route optimisation without mixing in my real vehicles or routes

  # Product toggle: ff_demo_mode
  # Demo mode is off by default in production AND debug — opt-in via Settings only.
  # When on: only the demo vehicle + demo route/stops are visible (same app functions).
  # When off: only real (non-demo) vehicles + routes are visible — hard isolation.
  # Real vs Demo differ only by data source / dataset — identical domain schema.
  # Demo stores are read-only. The only in-app "demo" label is a floating DEMO MODE pill.

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
    Then the active vehicle is 3.5 m high
    And the active route depot is DHL at 1A Boronia Road Brisbane Airport
    And the route has 2 pickup/load stops and 4 drop-off/unload stops
    And five stops are in industrial areas (Yatala, Lytton, or similar)
    And exactly one stop is in a residential area
    And at least one stop is near a verified low bridge (W5-8 / clearance dogfood)
    And route name and vehicle labels do not contain the word Demo

  Scenario: Demo and real data are hard-isolated via data source swap
    Given real vehicles and a real active route exist
    When Demo mode is turned on
    Then real vehicles are not listed
    And the real route is not the active route
    And only the demo dataset vehicle and route are available
    And the same Vehicle Route and TripStop types are used
    When Demo mode is turned off
    Then the demo dataset vehicle is not listed
    And the demo dataset route is not the active route
    And real vehicles and the real route are available again

  Scenario: Toggling Demo mode selects the demo data source
    Given real vehicles exist
    When the Settings Demo mode switch is turned on
    Then ff_demo_mode is on
    And the active route has the DHL depot and 6 seeded stops
    And only the 3.5 m dataset vehicle is listed
    And real vehicles are not listed
    When the Home map sheet is shown
    Then the route summary reflects 6 stops
    And the stop list shows the seeded dataset stops

  Scenario: Demo data source is read-only
    Given Demo mode is enabled
    When the driver tries to save a vehicle change
    Then the demo vehicle store is unchanged
    When the driver tries to replace the active route
    Then the demo route is unchanged
    When the driver tries to replace trip stops
    Then the demo trip stops are unchanged

  Scenario: Floating DEMO MODE pill is the only product demo label
    Given Demo mode is enabled
    Then a floating pill labelled DEMO MODE is shown above the app chrome
    And sheet content does not show Demo SEQ run or demo vehicle copy
    When Demo mode is turned off
    Then the DEMO MODE pill is hidden

  # Read-only demo must still dogfood product flows without Add stop / edit writes.
  Scenario: Seeded demo dataset is rich enough for read-only dogfood
    Given Demo mode is enabled
    Then the active route has verified start and end endpoints
    And trip stops all have verified coordinates
    And at least two stops carry pick-up cargo and two carry drop-off cargo
    And the active vehicle is 3.5 m high
    And at least one stop is near a verified low bridge
    And map markers height filter Optimize and Navigate can run without writes
    And with ff_map_route_polyline the home LineManager still draws PathRouter geometry
      (stop-near-bridge does not empty the route polyline — see map_route_polyline.feature)
