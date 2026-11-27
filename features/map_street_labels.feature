Feature: Prioritise street name labels on the map
  As a single-owner-operator driver
  I want road and street names visible at driving zooms
  So that I can navigate without POI clutter like bus stops crowding the map

  # Product toggle: ff_map_street_labels (requires ff_map_home).
  # Keeps OpenFreeMap Liberty style URL (OSS, free for commercial).
  # Runtime layer visibility / minzoom tweaks after style load — no paid tiles.

  Scenario: Street label priority is feature-flagged
    Given ff_map_home is enabled
    And ff_map_street_labels is off
    When the OpenFreeMap Liberty style loads
    Then basemap layer visibility is left at Liberty defaults
    When ff_map_street_labels is enabled
    Then runtime layer tweaks prefer highway name labels over POI labels

  Scenario: Street names are emphasised at driving zooms
    Given ff_map_street_labels is enabled
    And the map camera is at street-neighbourhood driving zoom
    Then highway-name-major and highway-name-minor layers are visible
    And highway-name-minor minzoom is at or below the driving zoom
    And the style URL remains OpenFreeMap Liberty

  Scenario: Bus stops and clutter POIs are de-emphasised
    Given ff_map_street_labels is enabled
    When the OpenFreeMap Liberty style loads
    Then the poi_transit layer is hidden
    And ranked POI label layers poi_r1 poi_r7 poi_r20 are hidden
    And airport IATA labels may remain for orientation
