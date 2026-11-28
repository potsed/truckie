Feature: Room-persist routes and stops
  As a single-owner-operator driver
  I want my active route and stops saved on-device with Room
  So that process death does not lose my tour and vehicles stay migrated safely

  # Product toggle: ff_room_route_persist (defaults on). ATNA-89 / P1-4.
  # Real AppDataSource ActiveTour reads/writes through Room. Demo stays
  # isolated read-only in-memory seed (same domain models). exportSchema on;
  # Migration 1→2 adds route/stop tables — destructive is not the only story.
  # Production Room builder does not allowMainThreadQueries; DAOs are suspend.
  # Out of scope: god-file splits (P1-5 / ATNA-90); Firebase.

  Scenario: Room route persist is feature-flagged and defaults on
    Given a fresh app start
    Then ff_room_route_persist is on
    And RealAppDataSource routes and trips are the same Room-backed ActiveTour facade
    And demo AppDataSource remains a separate read-only in-memory seed

  Scenario: Active route and stops survive a new Room session
    Given ff_room_route_persist is enabled
    And the driver creates a route with verified start, end, and stops
    When a new RoomActiveTour hydrates from the same database
    Then the active route id, endpoints, stop order, kinds, and cargo match
    And lastOptimizeMode is restored when previously set

  Scenario: Clearing the active tour clears Room
    Given ff_room_route_persist is enabled
    And an active route is persisted
    When the driver clears the active tour
    Then a fresh RoomActiveTour hydrates with no active route

  Scenario: Vehicle schema migrates without destructive-only
    Given an existing vehicles-only Room database at version 1
    When the app opens TruckieDatabase / VehicleDatabase at version 2
    Then Migration 1→2 adds routes, route_stops, route_cargo, and active_tour_meta
    And existing vehicle rows remain
    And exportSchema is enabled for the database
    And the production builder does not call allowMainThreadQueries
