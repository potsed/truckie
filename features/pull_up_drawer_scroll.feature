Feature: Home pull-up drawer scroll and insets
  As a single-owner-operator driver
  I want every stop in the home drag sheet to be reachable by scrolling
  So that later stops are not clipped by the screen bottom or system bars

  # Product toggle: ff_map_home (ships with map-home sheet; no new surface flag)
  # Fixes visible-height sheet layout (not a one-off bottom padding patch).
  # Out of scope: GraphHopper / optimised routing, Phase C vehicle depth.

  Scenario: Sheet height matches the on-screen pull-up area
    Given ff_map_home is enabled
    Then the route drag sheet is sized to its visible height for the current anchor
    And the sheet does not leave an off-screen scroll viewport below the home indicator

  Scenario: Stop list scrolls within the visible sheet with safe-area insets
    Given an active route with six or more stops on half or full sheet
    Then the stops list scrolls inside the visible sheet body
    And navigation-bar and IME insets pad the sheet content
    And the last stop can be scrolled fully on-screen above the home indicator

  Scenario: Jump to sample bridges FAB is not on the map
    Given ff_map_home and ff_demo_mode and ff_bridge_markers are enabled
    Then the home map does not show a floating Jump to sample bridges control
    And Demo mode remains available from Settings
