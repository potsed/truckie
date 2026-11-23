Feature: W5-8 only for low bridges
  As a single-owner-operator driver
  I want the AU W5-8 “LOW LEVEL BRIDGE” symbol only on restricted clearances
  So that high or unknown spans are not labelled as low bridges

  # Product toggle: ff_w5_8_low_only (also needs ff_bridge_markers)
  # Threshold: LowBridgeW58.LOW_CLEARANCE_MAX_M = 4.6 m — aligns with Queensland Rail’s
  # “Low clearance <4.6m” table band. QR road-under-rail rows with a published clearance
  # are height-restricted underpasses and qualify even when 4.6–5.4 m.
  # Not every bridge is a low bridge (ATNA-45).

  Scenario: W5-8 low-only policy is feature-flagged
    Given the feature flag ff_w5_8_low_only is off
    Then the low-only W5-8 filter is not active
    When ff_w5_8_low_only is enabled
    And ff_bridge_markers is enabled
    Then only qualifying low bridges receive the W5-8 symbol

  Scenario: Verified restricted clearances get W5-8
    Given ff_bridge_markers and ff_w5_8_low_only are enabled
    And the Australia-first pack is loaded
    Then Story Bridge (3.8 m verified BCC) qualifies for W5-8
    And Victoria Bridge (4.4 m verified BCC) qualifies for W5-8
    And Queensland Rail underbridges with a published clearance qualify for W5-8
    And a verified clearance at or below 4.6 m qualifies for W5-8

  Scenario: High open spans and unknown clearances do not get W5-8
    Given ff_w5_8_low_only is enabled
    When a crossing is unknown, not_applicable, or lacks a verified published height
    Then it does not receive the W5-8 low-level-bridge symbol
    And no height is invented for that crossing
    And Captain Cook / M3, William Jolly, Go Between, Goodwill, and Kurilpa are excluded

  Scenario: W5-8 is not used as generic bridge wording
    Given ff_w5_8_low_only is enabled
    When bridge markers are bound on the map
    Then only LowBridgeW58.bridgesForW58 entries use the AU W5-8 icon
    And the map does not present every pack bridge as a low bridge
