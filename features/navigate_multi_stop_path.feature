Feature: Navigate uses multi-stop tour path
  As a single-owner-operator driver
  I want Navigate to follow the same ordered PathRouter tour as the home polyline
  So that intermediate stops are not skipped after I tap Navigate

  # Product toggle: ff_navigate_multi_stop (defaults on). Requires ff_navigate.
  # Refactor (P1-2 / ATNA-87): one ActiveTourPath geometry for Navigate + map
  # polyline via MultiStopTourComposer ordered PathRouter legs. Start→end-only
  # Navigate path is deleted. Same ActiveTourSession SoT as ATNA-86.
  # With ff_route_path_from_location (ATNA-99): current → start → stops → end.
  # Out of scope: Room persist (P1-4), god-file splits (P1-5).
  # Async Valhalla / Nominatim: see features/async_routing_io.feature (ATNA-88 / P1-3).

  Scenario: Navigate multi-stop path is feature-flagged and defaults on
    Given a fresh app start
    Then ff_navigate_multi_stop is on
    And Navigate geometry comes from ActiveTourPath / MultiStopTourComposer
    And there is no start-to-end-only Navigate PathRouter path

  Scenario: Navigate path matches multi-stop composer geometry
    Given ff_navigate and ff_navigate_multi_stop are enabled
    And an active route with verified start, intermediate stops, and end
    And an active vehicle
    When the driver taps Navigate
    Then the Navigate path equals MultiStopTourComposer composeOrdered geometry for that route
    And the Navigate path is not a start-to-end-only stub that skips stops

  Scenario: Navigate prefers cached Optimise tour geometry
    Given ff_navigate and ff_navigate_multi_stop are enabled
    And Optimise has cached a multi-stop tour path for the active route
    When the driver taps Navigate
    Then Navigate uses that same optimised tour geometry

  Scenario: Navigate requires active vehicle for constrained tour
    Given ff_navigate and ff_navigate_multi_stop are enabled
    And an active route with endpoints
    And no active vehicle
    When the driver taps Navigate
    Then Navigate is blocked until an active vehicle is selected

  Scenario: Shared ActiveTourPath with map polyline
    Given ff_navigate_multi_stop and ff_map_route_polyline are enabled
    And the same route, vehicle, buffer, and composer
    Then Navigate and the home polyline resolve geometry through ActiveTourPath
    And both use ordered PathRouter legs (never DIY straight-lines)
