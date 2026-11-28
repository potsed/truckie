Feature: Offline map packs gated by ff_offline_maps
  As a single-owner-operator driver in poor coverage areas
  I want downloadable MapLibre offline map packs behind a feature flag
  So that Navigate and map home keep working without a live tile network

  # Product toggle: ff_offline_maps
  # Defaults OFF in production and debug — do not enable until OfflineManager +
  # pack download UX ship (select-user infra; see docs/feature-flags.md).
  # Contract only: MapLibre OfflineManager hooks; style must work offline.
  # Stub MapLibreStubEngine may fake packs in JVM tests; production native
  # engine reports pack manager unavailable until this flag lands a real path.
  # Out of scope for this feature file: enabling the flag in dogfood/debug,
  # Valhalla offline tiles (ff_valhalla_live / VALHALLA_BASE_URL), G-NAF packs.

  Scenario: Offline maps flag defaults off
    Given a fresh LocalFeatureFlags instance
    Then ff_offline_maps is off
    And Offline maps stay disabled even in debug dogfood defaults

  Scenario: Offline pack manager is hidden when the flag is off
    Given the feature flag ff_offline_maps is off
    And the MapLibre native engine is active
    Then the offline pack manager product surface is not exposed
    And listOfflinePackIds for the product gate is empty

  Scenario: Enabling the flag exposes the offline pack manager contract
    Given the feature flag ff_offline_maps is off
    When ff_offline_maps is enabled
    Then Offline maps are enabled for the product gate
    And the catalog targets MapLibre OfflineManager
    And pack download UX is required before production enablement
    And an offline-capable style is required before production enablement

  Scenario: JVM stub can register packs for tests without enabling production
    Given a MapLibreStubEngine
    When a pack id "qld-se" is registered for tests
    Then the stub reports the offline pack manager available
    And listOfflinePackIds contains "qld-se"
    And ff_offline_maps remains off by default on LocalFeatureFlags
