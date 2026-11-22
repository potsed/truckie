Feature: Downloadable Australia bridge pack
  As a single-owner-operator driver
  I want AU bridge markers from an updatable library pack
  So that clearance data stays current without a full app release

  # Ships with ff_bridge_markers. Baked-in sample JSON is bootstrap fallback only.
  # Long-term source of truth: versioned downloadable pack + local cache → SymbolManager.
  # Out of scope: live CDN hosting, full OSM/NHVR ingest, GraphHopper Custom Model.

  Scenario: Pack is the preferred source over the baked-in sample
    Given ff_bridge_markers is enabled
    And a cached AU bridge pack version is available
    When the home map loads bridge markers
    Then bridges come from the local pack cache
    And the baked-in sample asset is not used as the active source

  Scenario: Bootstrap sample seeds the cache when empty
    Given ff_bridge_markers is enabled
    And the local pack cache is empty
    When the home map loads bridge markers
    Then the Australia-first sample asset seeds the pack cache
    And subsequent loads read from the cache

  Scenario: Newer remote pack version updates the cache
    Given a cached pack at version "1.0.0"
    And a remote manifest advertises version "1.1.0" with AU bridges JSON
    When the pack repository refreshes
    Then the cache stores version "1.1.0"
    And SymbolManager loads the updated bridge points

  Scenario: Refresh keeps the cache when remote is not newer
    Given a cached pack at version "1.1.0"
    And a remote manifest advertises version "1.0.0"
    When the pack repository refreshes
    Then the cache remains at version "1.1.0"
