Feature: Async GraphHopper and Nominatim routing I/O
  As a single-owner-operator driver
  I want live GraphHopper and Nominatim work off the Compose main thread
  So that map home never ANRs when live routing or autocomplete is enabled

  # Product toggle: ff_async_routing_io (defaults on). ATNA-88 / P1-3.
  # Live PathRouter + Nominatim are suspend/async (IO dispatcher). Stub PathRouter
  # may stay instantaneous. Compose never calls live HTTP from remember or a
  # main-thread click path. Sync-on-main live path is deleted.
  # Do not enable ff_graphhopper_live in production defaults.
  # Out of scope: god-file splits (P1-5 / ATNA-90).
  # Room persist (P1-4 / ATNA-89): see room_route_persist.feature.

  Scenario: Async routing I/O is feature-flagged and defaults on
    Given a fresh app start
    Then ff_async_routing_io is on
    And live GraphHopper PathRouter path is suspend and hops off main
    And Nominatim suggest is suspend and hops off main
    And Compose remember does not call live HTTP for GH or Nominatim

  Scenario: Nominatim suggestions load asynchronously
    Given ff_address_autocomplete and ff_async_routing_io are enabled
    When the driver types a verified-address query on map home
    Then suggestions resolve via a coroutine / LaunchedEffect path
    And the Nominatim HTTP client is not invoked from Compose remember

  Scenario: Map polyline and Navigate use async PathRouter legs
    Given ff_async_routing_io is enabled
    And an active route with verified endpoints and vehicle
    When the home polyline resolves or the driver taps Navigate or Optimise
    Then PathRouter legs run through suspend MultiStopTourComposer / ActiveTourPath
    And MapRoutePolylineSession may cache the composed preview or Optimise path

  Scenario: Live GraphHopper stays off in production defaults
    Given a fresh production app start
    Then ff_graphhopper_live is off
    And PathRouter remains the stub until live is explicitly enabled with a base URL
