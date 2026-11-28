Feature: Single bridge passage policy and visibility pipeline
  As a single-owner-operator driver
  I want one shared over/under passage policy and one ordered marker filter pipeline
  So that home-map W5-8 symbols and PathRouter tour hard-blocks never disagree

  # Consolidation (ATNA-73 / audit P1): delete twin BridgePassageFilter vs
  # BridgePassageConstraint semantics; one BridgePassagePolicy in core-routing;
  # one BridgeVisibilityPipeline for Compose + style-layer bind.
  # Flags preserved: ff_bridge_passage, ff_w5_8_low_only, ff_vehicle_height_bridge_filter,
  # plus Qld open-data gates. Prefer wire passage in JSON; never invent heights.

  Scenario: Shared passage policy is the only conflict semantics
    Given BridgePassagePolicy lives in core-routing
    When the home-map filter and MultiStopTourComposer / BridgePassageConstraint decide under conflicts
    Then both reuse BridgePassagePolicy.heightConflictBlocksPath
    And driving over a low bridge deck is never blocked by under clearance
    And driving under is blocked only when clearance conflicts with vehicle height + buffer

  Scenario: BridgeVisibilityPipeline applies ordered stages once
    Given ff_bridge_markers and related clearance flags are enabled
    When home-map visible bridges are computed
    Then BridgeVisibilityPipeline runs stages in order:
      | stage           |
      | qld_open        |
      | w5_8_low        |
      | passage_under   |
      | vehicle_height  |
    And MapLibre style-layer bind receives the pre-filtered list
    And bindBridges does not re-apply LowBridgeW58 / passage / vehicle-height filters

  Scenario: Feature flags still gate each stage
    Given ff_bridge_passage is off
    Then the passage_under stage leaves the list unchanged
    Given ff_w5_8_low_only is off
    Then the w5_8_low stage leaves the list unchanged
    Given ff_vehicle_height_bridge_filter is off
    Then the vehicle_height stage leaves the list unchanged

  Scenario: Routing constraint facade delegates to the shared policy
    Given a RoutingBridgeConstraint for an under-restricting low clearance
    When BridgePassageConstraint.heightConflictBlocksPath is evaluated for an under path
    Then the result equals BridgePassagePolicy.heightConflictBlocksPath for the same inputs

  # Delivery next to a low structure is required (demo Wynnum / Colina St).
  # Through-path under-passage still hard-blocks; stop proximity alone does not.
  Scenario: Intentional stop near a low under-bridge does not hard-block the tour
    Given ff_bridge_passage is enabled with pack bridges
    And a tour stop lies within NEAR_PATH_M of a conflicting under-bridge
    When MultiStopTourComposer / BridgePassagePolicy evaluate path conflicts
    Then proximity at intentional stop coordinates is allowed (delivery)
    And intermediate path points near the same under-bridge still hard-block
    And ActiveTourPath / map polyline can still resolve PathRouter geometry
