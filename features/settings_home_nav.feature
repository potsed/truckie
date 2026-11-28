Feature: Settings Home returns to map home
  As a single-owner-operator driver
  I want tapping Home from Settings to open the map-first home
  So that I am not left on Settings with only the bottom bar gone

  # Product toggles: ff_nav_chrome + ff_map_home
  # Bug: bottom-nav saveState/restoreState + Activity redirect spaghetti left chrome
  # hidden for map home while the MapLibre destination did not remount cleanly.
  # Fix: one PrimaryNavigation path; Home remounts fresh when map home is on.
  # ATNA-74: legacy redirect Activities deleted — MainActivity NavHost is the only host.

  Scenario: Primary navigation is a single path
    Given nav chrome hosts primary tabs in one MainActivity NavHost
    Then Home Trip Vehicles and Settings share one PrimaryNavigation policy
    And MainActivity always hosts the NavHost regardless of flag combinations

  Scenario: Home from Settings remounts map home
    Given ff_map_home is enabled
    And the driver is on Settings with the global bottom bar
    When the driver taps Home
    Then navigation targets the home route
    And map home remounts fresh without restoreState
    And global TopAppBar and bottom NavigationBar stay hidden on map home
    And Settings is no longer the visible destination

  Scenario: Other primary tabs still preserve state
    Given ff_map_home is enabled
    When the driver switches from Home to Trip or Vehicles
    Then those tabs may save and restore state
    And returning to Home still remounts map home fresh

  Scenario: Chrome host gate is always the NavHost
    Given ff_map_home is enabled and ff_nav_chrome is off
    Then MainActivity still hosts the single-activity NavHost
    And Settings lives as a NavHost destination not a separate Activity
