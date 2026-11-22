Feature: Bridge markers on home map
  As a single-owner-operator driver
  I want low-bridge markers on the MapLibre home map
  So that I can see clearance constraints before planning a route

  # Product toggle: ff_bridge_markers
  # Long-term data: downloadable updatable AU pack (see bridge_pack.feature).
  # Baked-in sample asset is bootstrap only when the pack cache is empty.
  # Out of scope: Phase C vehicle UI, create-route changes, global header/footer,
  # live OSM/NHVR pack ingest host, GraphHopper Custom Model routing.

  Scenario: Bridge markers are feature-flagged
    Given the feature flag ff_bridge_markers is off
    Then the home map does not show bridge markers
    When ff_bridge_markers is enabled
    And ff_map_home is enabled
    Then bridge markers are visible on the MapLibre home map

  Scenario: Sample Australian bridge points load from a local asset
    Given ff_bridge_markers is enabled
    When the home map loads bridge fixture data
    Then bridges are loaded from a local Australia-first JSON asset
    And each bridge has an id, name, latitude, and longitude
    And clearance height is optional
    And several sample bridges sit within 2 km of Brisbane CBD fallback

  Scenario: Markers stay visible across map recompositions
    Given ff_bridge_markers is enabled
    And the MapLibre style has loaded once
    When location updates cause Compose to recompose the map
    Then the style is not reloaded
    And SymbolManagers are rebound on the live style so bridge markers remain visible

  Scenario: Markers use SymbolManager with optional clearance labels
    Given ff_bridge_markers is enabled
    And sample bridges include a height clearance
    Then bridge markers use SymbolManager not deprecated MarkerOptions
    And a short clearance label is shown when height data is present
    And bridges without height show a marker without a clearance label

  Scenario: Bridge markers stay Compose-only on map home
    Given ff_map_home and ff_bridge_markers are enabled
    Then bridge markers render on the map-first home MapLibre surface
    And no global header or footer is added for this feature
