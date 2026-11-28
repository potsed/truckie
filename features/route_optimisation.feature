Feature: Multi-stop route optimisation with PathRouter
  As a single-owner-operator driver
  I want Truckie to optimise stop order with truck-aware PathRouter routing
  So that I get a clearance-safe, cost-effective tour without driving under low bridges

  # ATNA-68 / ATNA-71 / ATNA-114. Optimisation + Navigate share one app-owned
  # PathRouter (ValhallaPathRouterFactory). Bridge passage: hard-block under when
  # clearance < height+buffer; over deck OK. Live HTTP behind ff_valhalla_live;
  # stub otherwise. UI on Trip stops only (ff_route_optimize).

  @ff_route_optimize @ff_multi_stop
  Scenario: Optimise stop order using PathRouter legs
    Given feature flag "ff_route_optimize" is enabled
    And a multi-stop trip with verified coordinates and an active vehicle
    When I optimise for "Fastest" with PathRouter tour composition
    Then the trip stop order is replaced with the PathRouter-optimised tour
    And the result includes a composed route path and truck model snippet

  @ff_route_optimize @ff_geographic_tour_order
  Scenario: Geographic tour keeps start/end and avoids farthest-first zig-zag
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_geographic_tour_order" is enabled
    And a SEQ-like round trip with zig-zag intermediate stop order
    When I optimise with PathRouter tour composition
    Then start and end stay anchored
    And the farthest stop is not the first intermediate after start
    And see features/geographic_tour_order.feature

  @ff_route_optimize @ff_bridge_passage
  Scenario: Under-passage low bridge hard-blocks a tour
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_bridge_passage" is enabled
    And an under-passage bridge clearance is below vehicle height plus buffer
    When PathRouter candidates would drive under that bridge
    Then those tours are rejected as unsafe
    And driving over a low bridge deck remains allowed

  @ff_route_optimize @ff_bridge_passage
  Scenario: Pack bridges are injected into the tour composer when passage is on
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_bridge_passage" is enabled
    And the active bridge pack has under-passage structures
    When the multi-stop tour composer is constructed
    Then pack bridges are mapped to RoutingBridgeConstraint and passed in
    And when ff_bridge_passage is off the composer receives an empty bridge list

  @ff_navigate @ff_valhalla_live @ff_route_optimize
  Scenario: Navigate and Optimise share the app PathRouter factory
    Given feature flag "ff_navigate" is enabled
    And feature flag "ff_route_optimize" is enabled
    When the app resolves PathRouter via ValhallaPathRouterFactory
    Then RouteNavigateSession uses that same PathRouter (not a hard-wired Stub)
    And MultiStopTourComposer uses that same PathRouter

  @ff_route_optimize @ff_valhalla_live
  Scenario: Live Valhalla is used when flag and credentials are set
    Given feature flag "ff_valhalla_live" is enabled
    And Valhalla credentials resolve a base URL
    When the path router factory resolves
    Then the live Valhalla PathRouter is selected

  @ff_valhalla_live
  Scenario: Stub PathRouter when live credentials are missing
    Given feature flag "ff_valhalla_live" is enabled
    And VALHALLA_API_KEY and VALHALLA_BASE_URL are unset
    When the path router factory resolves
    Then the stub PathRouter is selected

  @ff_route_optimize
  Scenario: Optimise UI remains gated and available on Trip stops
    Given feature flag "ff_route_optimize" is enabled
    Then "Optimize for this vehicle" is available on Trip stops
    And the optimised stop order summary can be shown after a successful run

  @ff_route_optimize
  Scenario: Default truck model dimensions are shared (no duplicated literals)
    Given a PathRouter is invoked without an explicit truck model
    Then stub and live routers both use TruckRoutingModel.default()
    And the default clearance is 4.0 m and default mass is 12 t
