Feature: Verified Brisbane CBD bridge clearances
  As a single-owner-operator driver
  I want sample CBD bridge clearances from authoritative AU sources
  So that I do not route on invented mock heights

  # Product toggle: ff_verified_cbd_clearances (also needs ff_bridge_markers)
  # Scope: current Brisbane CBD demo pack only — not full AU OSM/NHVR ingest (ATNA-11).
  # Local-first: verified figures ship in the bundled sample; bump pack version to re-seed.

  Scenario: Verified CBD clearances are feature-flagged
    Given the feature flag ff_verified_cbd_clearances is off
    Then verified clearance policy is not active
    When ff_verified_cbd_clearances is enabled
    And ff_bridge_markers is enabled
    Then clearance labels use only verified published heights

  Scenario: Mock demo heights are replaced with verified or omitted values
    Given ff_bridge_markers and ff_verified_cbd_clearances are enabled
    And the Australia-first CBD sample pack is loaded
    Then Story Bridge uses the BCC signed underpass clearance 3.8 m
    And Victoria Bridge uses the BCC signed underpass clearance 4.4 m
    And Captain Cook / M3, William Jolly, and Go Between omit height as unknown
    And Goodwill and Kurilpa are marked not_applicable (pedestrian/cycle)
    And no sample bridge invents a mock 4.x–5.0 m demo clearance

  Scenario: High open spans and vessel air draft are not road clearances
    Given ff_verified_cbd_clearances is enabled
    When a CBD crossing has no published signed road vertical clearance
    Then clearanceHeightM is omitted
    And clearanceStatus is unknown or not_applicable
    And MSQ vessel air draft above HAT is not used as a truck clearance

  Scenario: Pack metadata cites sources and retrieval date
    Given ff_verified_cbd_clearances is enabled
    When the bundled sample pack seeds the local cache
    Then pack metadata records authoritative sources
    And pack metadata records retrieval date 2026-11-23
    And the bootstrap pack version is newer than 0.3.0-sample so devices re-seed

  Scenario: Local-first storage is preserved
    Given ff_bridge_markers is enabled
    When the home map loads bridge markers
    Then loadActive reads on-device pack data only
    And no network fetch runs to resolve CBD clearances
