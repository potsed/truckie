Feature: Bridge over vs under passage
  As a single-owner-operator driver
  I want the app to know whether a structure restricts driving under it or is a deck I can drive over
  So that height conflicts block under-paths only — not every nearby road bridge deck

  # Product toggle: ff_bridge_passage (also needs ff_bridge_markers)
  # Passage: under | over | both | unknown
  # Routing rule (Daniel): OK to drive over a low bridge deck; not OK to drive under
  # when clearance conflicts with vehicle height + buffer.
  # QR underbridges → under. River road bridges (Story etc.) → primarily over for the
  # deck; published clearances apply to under paths (both). Never invent clearances.
  # ATNA-48

  Scenario: Bridge passage is feature-flagged
    Given the feature flag ff_bridge_passage is off
    Then the over/under passage filter is not active
    When ff_bridge_passage is enabled
    Then height conflicts apply only when the vehicle path goes under a restricted structure

  Scenario: Pack models under, over, both, and unknown
    Given the Australia-first bridge pack is loaded
    Then Queensland Rail road-under-rail rows have passage under
    And Story Bridge and Victoria Bridge have passage both (deck over + published under clearance)
    And Captain Cook / William Jolly / Go Between have passage over (road deck; no under clearance invented)
    And unknown is used only when passage cannot be classified without inventing data

  Scenario: Height conflict blocks under, not over
    Given ff_bridge_passage and ff_vehicle_height_bridge_filter are enabled
    And the active vehicle height plus buffer conflicts with a published under clearance
    Then driving under that structure is a conflict
    And driving over the same bridge deck is allowed
    And a pure over-crossing without under restriction is not a height conflict

  Scenario: Home-map markers respect under restriction
    Given ff_bridge_passage is enabled
    When bridge markers are filtered for height conflicts
    Then only structures that restrict under-passage can appear as clearance conflicts
    And clearance heights are never invented for unknown / not_applicable spans
