Feature: Route stop markers on home map
  As a single-owner-operator driver
  I want pickup and drop-off stops as markers on the MapLibre home map
  So that I can see the trip geography before satellite peek or Navigate

  # Product toggle: ff_stop_markers (requires ff_map_home).
  # Data: active route stops from AppDataSource (demo or real — same schema).
  # Rendering: shared GeoJSONSource + style symbol layer path with bridges
  # (MapStyleOverlayBind — not Annotation SymbolManager, not a parallel stack).
  # Out of scope: satellite peek (touch-and-hold), create-route changes, global chrome.

  Scenario: Stop markers are feature-flagged
    Given the feature flag ff_stop_markers is off
    Then the home map does not show route stop markers
    When ff_stop_markers is enabled
    And ff_map_home is enabled
    Then stop markers are visible on the MapLibre home map

  Scenario: Pickup and drop-off markers are distinct
    Given ff_stop_markers is enabled
    And the active route has pickup/load and drop-off/unload stops with coordinates
    Then pickup stops use the pickup style-layer icon
    And drop-off stops use the drop-off style-layer icon
    And stop markers use a GeoJSONSource symbol layer not deprecated MarkerOptions

  Scenario: Numbered stop sprites without overlapping text
    Given ff_stop_markers is enabled
    And the active route has pickup/load and drop-off/unload stops with coordinates
    When MapLibreHost binds stop overlays through the shared style-layer path
    Then each stop uses a numbered style image sprite (order index baked into the icon)
    And the stop symbol layer uses iconImage only (no textField over the sprite)
    And Annotation SymbolManager is not used
    And there is exactly one stop GeoJSON source and one stop symbol layer

  Scenario: Demo and real share AppDataSource stops
    Given ff_stop_markers is enabled
    When demo mode selects the demo AppDataSource
    Then stop markers come from the demo active route stops
    When demo mode is off
    Then stop markers come from the real AppDataSource active route
    And both use the same RouteStop schema and bind path

  Scenario: Markers stay visible across map recompositions
    Given ff_stop_markers is enabled
    And the MapLibre style has loaded once
    When location updates cause Compose to recompose the map
    Then stop markers rebind via the shared style-layer path with bridges
    And overlays recreate when style identity drifts or an overlay layer is missing

  Scenario: Stop markers stay Compose-only on map home
    Given ff_map_home and ff_stop_markers are enabled
    Then stop markers render on the map-first home MapLibre surface
    And no global header or footer is added for this feature
    And satellite peek is not implemented by this feature
