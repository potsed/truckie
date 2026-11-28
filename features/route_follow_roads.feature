Feature: Navigate and Optimise polylines follow road network geometry
  As a single-owner-operator driver
  I want Truckie route lines to follow the road network
  So that Navigate and Optimise show real road paths, not straight stubs

  # ATNA-113. Root cause on main: missing live credentials →
  # StubPathRouter linear interpolate (straight / stub geometry).
  # Engine: Valhalla truck costing. Interim: hosted Valhalla (Simple Routing).
  # Long-term: self-hosted / on-device Valhalla tiles. GraphHopper removed; Valhalla is the sole live PathRouter.
  # Flag: ff_valhalla_live (+ ff_async_routing_io). Docs: docs/valhalla-live.md

  @ff_valhalla_live @ff_map_route_polyline @ff_route_optimize @ff_navigate
  Scenario: Hosted Valhalla interim when API key is set
    Given feature flag "ff_valhalla_live" is enabled
    And feature flag "ff_async_routing_io" is enabled
    And VALHALLA_API_KEY is configured (secrets.properties / env / gradle)
    When ValhallaLiveCatalog resolves credentials
    Then effectiveBaseUrl defaults to https://api.simplerouting.io/valhalla
    And ValhallaPathRouterFactory selects LiveValhallaPathRouter with Bearer apiKey
    And RoutePath.source is valhalla-live (road-network geometry, truck costing)

  @ff_valhalla_live
  Scenario: Self-hosted or offline tiles swap leaves the same LiveValhallaPathRouter
    Given AU Valhalla tiles are available on a self-hosted or on-device host
    When VALHALLA_BASE_URL is set to https://vh.example.com
    And VALHALLA_API_KEY is cleared
    Then LiveValhallaPathRouter calls {base}/route without Authorization
    And ValhallaLiveCatalog.selfHostedSwapSteps documents the cutover

  @ff_valhalla_live
  Scenario: Missing credentials falls back to stub and surfaces degraded notice
    Given feature flag "ff_valhalla_live" is enabled
    And VALHALLA_API_KEY and VALHALLA_BASE_URL are unset
    When ValhallaPathRouterFactory resolves
    Then StubPathRouter is selected
    And ValhallaLiveCatalog reports degraded stub (not road network)
    And OptimizeResultSummary / Settings label the path as degraded stub
    And stub PathRouter geometry is colinear start→end interpolate

  @ff_valhalla_live
  Scenario: Live PathRouter decodes Valhalla encoded polylines (precision 6)
    Given a Valhalla /route trip response with leg shape strings
    When LiveValhallaPathRouter parses the response
    Then coordinates are decoded into RoutePath points (≥2)
    And RoutePath.source is valhalla-live
