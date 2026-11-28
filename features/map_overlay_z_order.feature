Feature: Map overlay z-order — stops above route above others
  As a single-owner-operator driver
  I want stop markers drawn above the route line, with bridges and other overlays below
  So that pickup/drop-off pins stay readable over the polyline and W5-8 markers

  # ATNA-122. Daniel: stop points top layer, route lines next, other layers below.
  # Use MapLibre native Style.addLayerAbove / addLayerBelow (not Annotation zIndex).
  # Existing product flags still gate each overlay; this flag gates the stack order.

  @ff_map_overlay_z_order @ff_stop_markers @ff_map_route_polyline @ff_bridge_markers
  Scenario: Truckie overlays stack stops above route above bridges
    Given feature flag "ff_map_overlay_z_order" is enabled
    And bridge, stop, and route style layers are bound on the live MapLibre style
    When MapStyleOverlayBind reconciles overlay z-order
    Then style layer order bottom-to-top places bridges below the route line
    And the route line is below the stop symbol layer
    And stop points are the topmost Truckie overlay layer

  @ff_map_overlay_z_order
  Scenario: Layer placement uses MapLibre native addLayerAbove
    Given feature flag "ff_map_overlay_z_order" is enabled
    When a Truckie overlay layer is ensured on the style
    Then MapLibre Style.addLayerAbove / addLayerBelow places it relative to siblings
    And Annotation manager zIndex is not used

  @ff_map_overlay_z_order
  Scenario: Overlay z-order is feature-flagged
    Given feature flag "ff_map_overlay_z_order" is disabled
    Then MapOverlayZOrderCatalog reports disabled
    And when the flag is enabled the catalog reports enabled with native layer ordering
