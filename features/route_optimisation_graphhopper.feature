Feature: Multi-stop route optimisation with GraphHopper
  As a single-owner-operator driver
  I want Truckie to optimise stop order with truck-aware GraphHopper routing
  So that I get a clearance-safe, cost-effective tour without driving under low bridges

  # ATNA-68 / ATNA-71. Optimisation + Navigate share one app-owned PathRouter
  # (GraphHopperPathRouterFactory). Bridge passage: hard-block under when
  # clearance < height+buffer; over deck OK. Live HTTP behind ff_graphhopper_live;
  # stub otherwise. UI on Trip stops only (ff_route_optimize).
  # Sibling workers own pack merge + BridgePassage UI — do not edit those here.

  @ff_route_optimize @ff_multi_stop
  Scenario: Optimise stop order using GraphHopper legs
    Given feature flag "ff_route_optimize" is enabled
    And a multi-stop trip with verified coordinates and an active vehicle
    When I optimise for "Fastest" with GraphHopper tour composition
    Then the trip stop order is replaced with the GraphHopper-optimised tour
    And the result includes a composed route path and Custom Model snippet

  @ff_route_optimize @ff_bridge_passage
  Scenario: Under-passage low bridge hard-blocks a tour
    Given feature flag "ff_route_optimize" is enabled
    And feature flag "ff_bridge_passage" is enabled
    And an under-passage bridge clearance is below vehicle height plus buffer
    When GraphHopper candidates would drive under that bridge
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

  @ff_navigate @ff_graphhopper_live @ff_route_optimize
  Scenario: Navigate and Optimise share the app PathRouter factory
    Given feature flag "ff_navigate" is enabled
    And feature flag "ff_route_optimize" is enabled
    When the app resolves PathRouter via GraphHopperPathRouterFactory
    Then RouteNavigateSession uses that same PathRouter (not a hard-wired Stub)
    And MultiStopTourComposer uses that same PathRouter

  @ff_route_optimize @ff_graphhopper_live
  Scenario: Live GraphHopper is used when flag and base URL are set
    Given feature flag "ff_graphhopper_live" is enabled
    And a GraphHopper base URL is configured
    When the path router factory resolves
    Then the live GraphHopper PathRouter is selected

  @ff_graphhopper_live
  Scenario: Stub GraphHopper when live flag is off
    Given feature flag "ff_graphhopper_live" is disabled
    When the path router factory resolves
    Then the stub GraphHopper PathRouter is selected

  @ff_route_optimize
  Scenario: Optimise UI remains gated and available on Trip stops
    Given feature flag "ff_route_optimize" is enabled
    Then "Optimize for this vehicle" is available on Trip stops
    And the optimised stop order summary can be shown after a successful run

  @ff_route_optimize
  Scenario: Default Custom Model dimensions are shared (no duplicated literals)
    Given a PathRouter is invoked without an explicit Custom Model
    Then stub and live routers both use GraphHopperCustomModelStub.default()
    And the default clearance is 4.0 m and default mass is 12 t
