Feature: Downloadable Australia bridge pack
  As a single-owner-operator driver
  I want AU bridge markers from an updatable library pack
  So that clearance data stays current without a full app release

  # Ships with ff_bridge_markers. Baked-in sample JSON is bootstrap fallback only.
  # Long-term source of truth: versioned downloadable pack + local cache → style symbol layer.
  # Local-first / power efficiency: loadActive never networks; refresh downloads only
  # when the remote version is newer. Live OSM/NHVR ingest stays ATNA-11.
  # Out of scope: live CDN hosting, full OSM/NHVR ingest, truck routing model.

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
    And no network fetch runs during loadActive

  Scenario: Pack loads local-first without repeated network fetches
    Given ff_bridge_markers is enabled
    And the AU sample pack is seeded on disk
    When the home map loads bridge markers repeatedly
    Then each load reads the on-device cache
    And the pack downloader is not invoked

  Scenario: Newer remote pack version updates the cache
    Given a cached pack at version "1.0.0"
    And a remote manifest advertises version "1.1.0" with AU bridges JSON
    When the pack repository refreshes
    Then the cache stores version "1.1.0"
    And the style symbol layer loads the updated bridge points

  Scenario: Refresh keeps the cache when remote is not newer
    Given a cached pack at version "1.1.0"
    And a remote manifest advertises version "1.1.0" or "1.0.0"
    When the pack repository refreshes
    Then the cache remains at version "1.1.0"
    And the pack downloader is not invoked again

  # ATNA-72 — PackChannel + BridgePackMerge: never full-replace curated
  # bootstrap with a thinner cross-channel OSS/published body.
  Scenario: OSS refresh merges into curated bootstrap instead of clobbering
    Given ff_bridge_markers is enabled
    And the local pack cache is seeded at version "0.7.0-sample" with curated CBD QR TMR BCC rows
    And a remote PUBLIC_OSS manifest advertises version "0.1.0" with a thinner bridges JSON
    When the pack repository refreshes
    Then BridgePackMerge applies a merge-by-id overlay
    And curated bootstrap bridge ids remain in the on-device cache
    And remote OSS rows are present by id
    And the cache stamp advances to the remote version
