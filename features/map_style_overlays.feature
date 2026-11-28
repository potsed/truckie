Feature: MapLibre style-layer overlays for bridges, stops, and route
  As a single-owner-operator driver
  I want bridges, stops, and the route polyline drawn via MapLibre GeoJSONSource + style layers
  So that overlays stay visible without Annotation SymbolManager / LineManager races

  # ATNA-117: prefer map features / style layers over Annotation managers.
  # One clear path — delete Annotation bind when the layer path lands (refactor).
  # Parity: same height filter, over/under passage, packs, W5-8 look, stop icons,
  # route line colour/width, OpenFreeMap Noto text, Recenter/follow untouched.
  # Existing product flags still gate each overlay:
  #   ff_bridge_markers, ff_stop_markers, ff_map_route_polyline (+ filter flags).

  Scenario: Overlays use GeoJSONSource and style layers not Annotation managers
    Given ff_map_home is enabled
    When bridge, stop, and route overlays bind on the live MapLibre style
    Then each overlay uses a GeoJSONSource plus a style symbol or line layer
    And Annotation SymbolManager and LineManager are not used
    And the deprecated MarkerOptions / addMarker API is not used

  Scenario: Bridge W5-8 parity with prior SymbolManager look
    Given ff_bridge_markers is enabled
    And BridgeVisibilityPipeline has filtered pack bridges (height, over/under, packs)
    When MapLibreHost binds bridge overlays
    Then GeoJSON point features carry the pre-filtered bridges only
    And the symbol layer uses the AU W5-8 icon at the shared neighbourhood iconSize
    And W5-8 iconSize is 1.25× the ATNA-118 size (0.5× baseline × 1.25 × 1.25; relative sizing vs stops kept)
    And verified clearance labels use Noto Sans Regular with textOptional
    And bind does not re-apply LowBridgeW58 / passage / vehicle-height filters
    And Annotation SymbolManager is not used

  Scenario: Stop markers use numbered sprites on one symbol layer
    Given ff_stop_markers is enabled
    And the active route has pickup and drop-off stops with coordinates
    When MapLibreHost binds stop overlays
    Then pickup and drop-off use distinct numbered icon-image property values
    And stop order indices are baked into style image sprites (not a textField overlay)
    And the stop symbol layer does not set textField (avoids doubled/scribbled icons)
    And MapLibreHost applies overlays through one applyOverlays path (no duplicate binds)
    And stops share the same style-layer bind path as bridges

  Scenario: Route polyline parity with prior LineManager look
    Given ff_map_route_polyline is enabled
    And ActiveTourPath / MapRoutePolylineGeometry resolves at least two points
    When MapLibreHost binds the route overlay
    Then a GeoJSON LineString source feeds a style line layer
    And line colour and width match the prior MapRoutePathStyle values
    And Navigate and home polyline share that same style-layer path

  Scenario: Style identity drift rebinds overlays without orphaning
    Given the MapLibre style has loaded once
    When Compose recomposes or the live style identity drifts
    Then setStyle is not started a second time while loading
    And missing overlay layers are recreated on the live style
    And Recenter / follow LocationComponent behaviour is unchanged

  # ATNA-122 — detail in features/map_overlay_z_order.feature
  @ff_map_overlay_z_order
  Scenario: Overlay stack keeps stops above route above bridges
    Given bridge, stop, and route style layers are bound
    When MapLibreHost applyOverlays reconciles z-order
    Then MapLibre native layer order is stops top, route next, bridges below
