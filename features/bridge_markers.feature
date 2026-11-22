Feature: Bridge markers on home map
  As a single-owner-operator driver
  I want low-bridge markers on the MapLibre home map
  So that I can see clearance constraints before planning a route

  # Product toggle: ff_bridge_markers
  # Australia-first sample/fixture points from a local asset (not live GraphHopper).
  # Out of scope: Phase C vehicle UI, create-route changes, global header/footer,
  # live OSM/NHVR pack ingestion, GraphHopper Custom Model routing.

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
