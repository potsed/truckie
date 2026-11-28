Feature: Bridge-safe route
  As a driver
  I want routes that respect effective clearance and mass
  So that I avoid low bridges and weight-restricted structures

  # Product toggle: ff_bridge_safe_routing (future Custom Model).
  # ATNA-92: SafeRouteSelector deleted — production path is MultiStopTourComposer
  # + PathRouter / BridgePassagePolicy (and ff_path_block_detour). RouteCandidate
  # remains as the composer scoring type via TourPathSelector.

  @ff_bridge_safe_routing
  Scenario: Unsafe clearance is rejected on the composer PathRouter path
    Given feature flag "ff_bridge_safe_routing" is enabled
    And the effective clearance height is 3.65 m
    When PathRouter returns a tour that under-passes a 3.4 m structure
    Then MultiStopTourComposer rejects or detours the unsafe under-path
    And SafeRouteSelector is not part of the production routing story
