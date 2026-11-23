Feature: Queensland TMR and BCC structure clearance packs
  As a single-owner-operator driver
  I want TMR Structure Clearances and BCC Road Height Restrictions in the AU pack
  So that QLD low bridges show published GIS heights without invented mocks

  # Product toggles: ff_tmr_structure_clearances, ff_bcc_road_height_restrictions
  # (also need ff_bridge_markers). W5-8 remains gated by ff_w5_8_low_only.
  # Sources (retrieved 2026-11-23):
  # - TMR Structure Clearances: https://www.data.qld.gov.au/dataset/structure-clearances-queensland
  #   (ArcGIS StateRoadInformation layers 21 <4.6 m and 22 ≥4.6–<5.4 m)
  # - BCC Road Height Restrictions: https://data.brisbane.qld.gov.au/explore/dataset/road-restrictions/
  # Prefer TMR/BCC published figures over invented heights.
  # Merge with existing QR SEQ + BCC CBD verified rows; de-dupe by id / source id / coords.
  # Physical roadside signs override pack data; clearance buffer covers data lag vs signs.
  # Out of scope: full NHVR national map / other states (ATNA-43).

  Scenario: TMR structure clearances are feature-flagged
    Given the feature flag ff_tmr_structure_clearances is off
    Then TMR Structure Clearance pack entries are not shown on the map
    When ff_tmr_structure_clearances is enabled
    And ff_bridge_markers is enabled
    Then TMR structure clearance markers are shown with verified published heights

  Scenario: BCC road height restrictions are feature-flagged
    Given the feature flag ff_bcc_road_height_restrictions is off
    Then BCC GIS road-restriction pack entries are not shown on the map
    When ff_bcc_road_height_restrictions is enabled
    And ff_bridge_markers is enabled
    Then BCC local bridge/tunnel/ped-overpass markers under 4.6 m are shown

  Scenario: Pack merges TMR and BCC with existing QR and CBD verified rows
    Given ff_bridge_markers, ff_queensland_rail_clearances, ff_verified_cbd_clearances,
      ff_tmr_structure_clearances, and ff_bcc_road_height_restrictions are enabled
    And the Australia-first pack is loaded
    Then Story Bridge and Victoria Bridge keep the verified CBD BCC clearances
    And Queensland Rail SEQ road-under-rail rows remain present
    And TMR Structure Clearance rows cite the data.qld / StateRoadInformation source
    And BCC GIS rows cite the Brisbane Open Data road-restrictions dataset
    And no height is invented for a structure absent from those sources

  Scenario: De-duplicate by id, source id, and coordinates
    Given the merged AU pack is seeded
    Then no two pack entries share the same id
    And BCC GIS rows that match an existing CBD clearanceSourceId are not duplicated
    And entries within ~35 m of an existing pack point are de-duplicated

  Scenario: W5-8 only for low clearances or QR underbridges
    Given ff_w5_8_low_only is enabled
    When TMR or BCC pack rows are considered for W5-8
    Then only verified heights ≤ 4.6 m qualify
    And QR published underbridges still qualify per the existing underbridge rule
    And TMR mid-band clearances above 4.6 m do not get W5-8

  Scenario: Pack metadata cites TMR and BCC source URLs and retrieval date
    Given ff_tmr_structure_clearances and ff_bcc_road_height_restrictions are enabled
    When the bundled sample pack seeds the local cache
    Then pack metadata records the TMR Structure Clearances dataset URL
    And pack metadata records the BCC road-restrictions dataset URL
    And pack metadata records retrieval date 2026-11-23
    And the bootstrap pack version is newer than 0.5.0-sample so devices re-seed

  Scenario: Local-first storage and signage override are preserved
    Given ff_bridge_markers is enabled
    When the home map loads bridge markers
    Then loadActive reads on-device pack data only
    And no network fetch runs to resolve TMR or BCC clearances
    And documentation states physical roadside signs override digital packs
    And the clearance buffer exists in part for data lag versus signs
