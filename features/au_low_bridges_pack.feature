Feature: Australia low-bridges open source pack (Truckie source pack)
  As a single-owner-operator driver
  I want Truckie to consume the shared AU low-bridges JSON pack local-first
  So that clearances stay current without a thinner parallel schema

  # ATNA-50 — ff_au_low_bridges_pack. Canonical JSON from potsed/au-low-bridges.
  # Required: id, lat/lng, clearanceHeightM, source{name,url,retrievedAt}.
  # Recommended: passage, name, roadUnder/roadOver, state/LGA, weight/signage notes.
  # Prefer null/unknown over inventing. Roadside signs override digital packs.
  # loadActive never networks; refresh downloads only when remote version is newer.

  Scenario: Shared pack schema fields parse without inventing
    Given ff_bridge_markers is enabled
    And a bridges JSON row includes source name URL retrievedAt roadUnder and clearanceHeightM
    When Truckie parses the pack JSON
    Then the BridgePoint keeps clearanceHeightM and source fields
    And missing lga or weightRestriction stay null

  Scenario: Remote pack refresh is gated by feature flag
    Given ff_au_low_bridges_pack is disabled
    When the app starts
    Then the bridge pack downloader stays NoOp
    And no remote au-low-bridges refresh runs

  Scenario: Flag on uses HTTPS pack URL and refreshes only when newer
    Given ff_au_low_bridges_pack is enabled
    And the local cache is at bootstrap sample version
    And the remote au-low-bridges manifest advertises version "1.0.0"
    When the pack repository refreshes from the OSS pack URL
    Then the on-device cache stores version "1.0.0"
    And subsequent loadActive reads local cache without networking

  # ATNA-72 — cross-channel refresh must not wipe curated dogfood rows.
  Scenario: Thinner OSS 0.1.0 does not clobber curated 0.7.0-sample
    Given ff_au_low_bridges_pack is enabled
    And the local cache holds curated bootstrap "0.7.0-sample"
    And the remote OSS pack is version "0.1.0" with fewer bridges than bootstrap
    When the pack repository refreshes from AuLowBridgesPack.remoteManifest
    Then PackChannel treats bootstrap as CURATED_SAMPLE and remote as PUBLIC_OSS
    And BridgePackMerge merges by id instead of full replace
    And verified curated rows that are absent from OSS remain cached
