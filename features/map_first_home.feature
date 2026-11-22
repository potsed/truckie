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

  Scenario: Default camera prefers the driver's current location
    Given map camera defaults for Truckie
    And the official Play Services Fused Location Provider stack is used
    When location permission is granted and Settings can satisfy a fused request
    Then the map camera centres on the driver's current location
    When location is unavailable, permission is denied, or Settings resolution is cancelled
    Then the map camera falls back to Brisbane Australia
    And the default zoom shows a street-neighbourhood view

  Scenario: Blue my-location puck follows the same Fused Location fix
    Given ff_map_home and ff_map_location_puck are enabled
    And the official Play Services Fused Location Provider stack is used
    When location permission is granted and a current fix is available
    Then a standard blue location indicator is shown at that fix
    And the map camera centres on that same fix
    And the puck shows a subtle repeating LocationComponent pulse
    And the puck updates as location updates arrive
    When location permission is denied or a fix is unavailable
    Then the blue location indicator is hidden

  Scenario: Follow mode keeps the puck centred until the driver pans away
    Given ff_map_home and ff_map_location_puck are enabled
    And location permission is granted with a current fix
    Then follow mode is on by default while location is centred
    And the blue my-location puck stays locked at map centre
    And the map camera moves as the driver moves so the puck remains centred
    And the Recenter control is hidden while following
    When the driver pans the map so location is no longer centred
    Then follow mode exits
    And a Recenter control is shown on the map overlay
    When the driver taps Recenter
    Then the camera animates back onto the current location
    And zoom is reset to the default closer zoom used when first centering on the puck
    And follow mode is re-enabled
    And the Recenter control is hidden while centred and following

  Scenario: Map style renders a street basemap
    Given MapLibre map home is shown
    Then the style URL is OpenFreeMap Liberty street tiles
    And demotiles is not used as the default style
    And stop markers use SymbolManager not deprecated MarkerOptions

  Scenario: Drag sheet supports peek half and full
    Given ff_map_home is enabled
    Then the route sheet exposes peek, half, and full anchors
    And the sheet responds to drag gestures via AnchoredDraggable with a Material3 drag handle
    And the peek state shows a summary strip over the map
    And half and full reveal create-route and stop list content
    # Visual polish tokens: features/route_sheet_polish.feature

  Scenario: Map home hides global header and footer chrome
    Given ff_map_home is enabled
    Then the map home destination has no TopAppBar or bottom NavigationBar
    And Trip Vehicles and Settings still show global chrome when opened
    And a map overlay menu reaches those destinations

  Scenario: Create route with optional name and date
    Given ff_map_home is enabled
    When the driver creates a route with an optional name
    Then they can choose Today, Tomorrow, or a custom date
    And the route is stored in the Route domain session

  Scenario: Stops are pickup load or drop-off unload
    Given an active route on the map home sheet
    When the driver adds stops via the add-or-find stops UI
    Then each stop is pickup/load or drop-off/unload
    And address autocomplete is NoOp unless ff_address_autocomplete is on
    And the stop list and summary strip update

  Scenario: Map home stays Compose-only on the single-activity NavHost
    Given ff_map_home and ff_nav_chrome are enabled
    Then Home content is Compose Material3 with no XML layout
    And the map home is hosted inside the MainActivity NavHost
    And high-contrast navy chrome remains readable
