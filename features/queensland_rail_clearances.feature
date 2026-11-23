Feature: Queensland Rail SEQ rail underbridge clearances
  As a single-owner-operator driver
  I want verified road-under-rail clearances from Queensland Rail in the AU pack
  So that SEQ low rail bridges show real signed heights without invented mocks

  # Product toggle: ff_queensland_rail_clearances (also needs ff_bridge_markers)
  # Source: https://www.queenslandrail.com.au/forcustomers/safetysecurity/railbridges
  # Local-first: QR figures ship in the bundled sample; bump pack version to re-seed.
  # Keep verified BCC CBD river-crossing data (ATNA-42 / PR #31) where still valid.

  Scenario: Queensland Rail clearances are feature-flagged
    Given the feature flag ff_queensland_rail_clearances is off
    Then Queensland Rail pack entries are not shown on the map
    When ff_queensland_rail_clearances is enabled
    And ff_bridge_markers is enabled
    Then Queensland Rail road-under-rail markers are shown with verified heights

  Scenario: Pack ingests only published QR clearances with accurate locations
    Given ff_bridge_markers and ff_queensland_rail_clearances are enabled
    And the Australia-first pack is loaded
    Then every Queensland Rail entry matches a published suburb/road/clearance from the QR page
    And each Queensland Rail marker sits on the published location map coordinates
    And no height is invented for a bridge that is not on the QR page

  Scenario: Prefer QR figures over unknown for matching rail assets
    Given ff_queensland_rail_clearances is enabled
    When a pack asset is a Queensland Rail road-under-rail crossing
    Then clearanceStatus is verified
    And clearanceHeightM is the published QR road clearance
    And clearanceSource cites the Queensland Rail railbridges URL

  Scenario: BCC CBD river-crossing clearances remain valid
    Given ff_verified_cbd_clearances is enabled
    And the bundled sample pack seeds the local cache
    Then Story Bridge keeps the BCC signed underpass clearance 3.8 m
    And Victoria Bridge keeps the BCC signed underpass clearance 4.4 m
    And Captain Cook / M3, William Jolly, and Go Between remain unknown without invented height
    And Goodwill and Kurilpa remain not_applicable

  Scenario: Pack metadata cites QR source URL and retrieval date
    Given ff_queensland_rail_clearances is enabled
    When the bundled sample pack seeds the local cache
    Then pack metadata records the Queensland Rail railbridges source URL
    And pack metadata records retrieval date 2026-11-23
    And the bootstrap pack version is newer than 0.4.0-sample so devices re-seed

  Scenario: Local-first storage is preserved
    Given ff_bridge_markers is enabled
    When the home map loads bridge markers
    Then loadActive reads on-device pack data only
    And no network fetch runs to resolve Queensland Rail clearances
