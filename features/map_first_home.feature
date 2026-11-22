Feature: Map-first home with route stop drag sheet
  As a single-owner-operator driver
  I want a MapLibre map home with a bottom drag sheet for routes and stops
  So that I can create a route, add pickup/load and drop-off/unload stops, and see them on the map

  # Product toggle: ff_map_home
  # Compose Material3 only; single-activity NavHost primary path.
  # Out of scope: live GraphHopper, tolls, load optimisation, real address provider.

  Scenario: Map home is feature-flagged
    Given the feature flag ff_map_home is off
    Then the primary home keeps the list-first summary screen
    When ff_map_home is enabled
    Then Home uses the map-first Compose screen with MapLibre

  Scenario: Default camera is Australia Brisbane
    Given map camera defaults for Truckie
    Then the default latitude and longitude centre on Brisbane Australia
    And the default zoom shows a metro overview

  Scenario: Drag sheet supports peek half and full
    Given ff_map_home is enabled
    Then the route sheet exposes peek, half, and full anchors
    And the peek state shows a summary strip over the map
    And half and full reveal create-route and stop list content

  Scenario: Create route with optional name and date
    Given ff_map_home is enabled
    When the driver creates a route with an optional name
    Then they can choose Today, Tomorrow, or a custom date
    And the route is stored in the Route domain session

  Scenario: Stops are pickup load or drop-off unload
    Given an active route on the map home sheet
    When the driver adds stops via the add-or-find stops UI
    Then each stop is pickup/load or drop-off/unload
    And address autocomplete may be a NoOp stub until a provider ships
    And the stop list and summary strip update

  Scenario: Map home stays Compose-only on the single-activity NavHost
    Given ff_map_home and ff_nav_chrome are enabled
    Then Home content is Compose Material3 with no XML layout
    And the map home is hosted inside the MainActivity NavHost
    And high-contrast navy chrome remains readable
