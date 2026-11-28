Feature: Owned AU address pack (G-NAF source of truth)
  As a single-owner-operator driver
  I want stop geocoding from Truckie's versioned G-NAF address pack
  So that Navigate uses premise-precise G-NAF coords and high-quality formatted addresses

  # ATNA-109 / ATNA-111 — ff_owned_address_pack.
  # G-NAF data first only — one G-NAF-backed pack/search path.
  # Do not merge other address datasets (OpenAddresses, OSM, etc.).
  # Nominatim = typeahead UI at most until G-NAF resolves the hit (not SoT).
  # JSON primary; optional Room/FTS derived. No national G-NAF in the APK.
  # QLD-first: downloadable region pack (Kallangur dogfood slice → full QLD).

  Scenario: Owned address pack is feature-flagged
    Given the feature flag ff_owned_address_pack is off
    Then the owned address pack index is not consulted for autocomplete
    When ff_owned_address_pack is enabled
    And a local G-NAF pack covers the query
    Then autocomplete resolves the hit from the G-NAF pack premise
    And Nominatim must not supply the Navigate coordinate for that hit

  Scenario: Pack row carries required G-NAF geocode and provenance fields
    Given an addresses JSON row with id lat lon formatted number street locality state postcode precision and G-NAF source
    When Truckie parses the owned address pack
    Then the AddressPackRecord keeps those fields without inventing missing unit
    And precision is unit house or building for SoT rows
    And id is the G-NAF ADDRESS_DETAIL_PID

  Scenario: Autocomplete yields a verified Place for Navigate from G-NAF
    Given ff_address_autocomplete and ff_owned_address_pack are enabled
    And the local G-NAF pack includes unit U21 at a G-NAF premise point
    When the driver selects that suggestion
    Then the RouteStop stores the formatted label with non-null latitude and longitude
    And the coordinates match the G-NAF pack premise not a Nominatim street centroid

  Scenario: Nominatim is typeahead only until G-NAF resolves
    Given ff_owned_address_pack is enabled
    When the driver types before a G-NAF hit is resolved
    Then Nominatim may contribute typeahead labels only
    And Navigate must wait for or use the G-NAF-resolved premise
    And public OSM incompleteness must not become SoT

  Scenario: Remote pack refresh is local-first like bridges
    Given ff_owned_address_pack is enabled
    And the local cache is at bootstrap sample version
    And the remote G-NAF address pack manifest advertises a newer version
    When the pack repository refreshes
    Then the on-device cache stores the newer version
    And subsequent loadActive reads local cache without networking

  Scenario: QLD G-NAF ETL produces owned pack schema
    Given a G-NAF extract filtered to QLD
    When the G-NAF ETL runs
    Then the output pack region is QLD
    And each row id is an ADDRESS_DETAIL_PID
    And each row has premise lat lon unit-or-number street locality state postcode precision and G-NAF provenance
    And no non-G-NAF address rows are present

  Scenario: Kallangur U21 resolves from the owned QLD pack
    Given ff_owned_address_pack is enabled
    And the active pack includes GAQLD163067541 for U21 1-31 Elsie Street Kallangur
    When the driver types U21 1-31 Elsie Street Kallangur
    Then the top verified suggestion uses G-NAF coords -27.25532783 152.99620218
    And placeId is GAQLD163067541
    And Nominatim street centroids are not used for that hit
