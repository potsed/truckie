Feature: Active tour single source of truth
  As a single-owner-operator driver
  I want map-home create-route, Plan Trip, Optimise, and demo to share one active tour
  So that stop order and contents never diverge between Route and Trip stores

  # Product toggle: ff_active_tour (defaults on)
  # Refactor (P1-1 / ATNA-86): one ActiveTourSession facade. Dual RouteSession +
  # TripSession path is deleted. TripStop remains a projection for Optimise /
  # clearance / mass (MultiStopTourComposer).
  # Out of scope: async GraphHopper (P1-3), Room persist (P1-4), god-file splits (P1-5).
  # Navigate multi-stop (P1-2 / ATNA-87): see navigate_multi_stop_path.feature.

  Scenario: Active tour is feature-flagged and defaults on
    Given a fresh app start
    Then ff_active_tour is on
    And map-home, Plan Trip, Optimise, and demo share one ActiveTour facade
    And there is no parallel RouteSession plus TripSession wiring

  Scenario: Map-home create-route and Plan Trip share the same stops
    Given ff_active_tour is enabled
    And the driver creates a route on the map-home sheet with verified start and end
    When the driver adds a pickup/load stop from the map sheet
    Then Plan Trip lists that same stop
    When the driver adds a stop from Plan Trip
    Then the map-home active route includes that stop

  Scenario: Optimise reorders the shared active tour
    Given ff_active_tour is enabled
    And an active tour with multiple verified stops
    When Optimise replaces the stop order
    Then the map-home route stops match the optimised order
    And Plan Trip stops match the same optimised order
    And lastOptimizeMode is stored on the shared facade

  Scenario: Demo seeds one tour projected to TripStop
    Given ff_active_tour and ff_demo_mode are enabled
    Then the demo AppDataSource exposes the same stop ids on routes and trips
    And routes and trips are the same facade instance
    And TripStop cargo is projected from the active Route stops
    And demo writes remain read-only on the shared facade

  Scenario: TripStop is a projection not a parallel SoT
    Given ff_active_tour is enabled
    When the active Route stop list changes
    Then TripRepository.stops reflects the Route stops via TripStopProjection
    And Route remains the single editable source of truth
