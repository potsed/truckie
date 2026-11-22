Feature: Jetpack Compose UI
  As a driver
  I want every Truckie1 screen rendered with Jetpack Compose Material3
  So that UI is readable, edge-to-edge, and free of obsolete XML/AppCompat screen debt

  # Platform UI stack (not a product toggle). Existing product flags still gate entry points.
  # High-contrast Home intent from PR #6: white labels on navy fills; no DarkActionBar strip.

  Scenario: Compose Material3 theme keeps high-contrast primary actions
    Given the Truckie1 Compose Material3 color scheme is defined
    Then primary container "navy" and on-primary "white" meet WCAG AA contrast
    And body text "navy" on background "sky" meets WCAG AA contrast

  Scenario: Activities use Compose setContent without ActionBar chrome
    Given every screen Activity hosts Compose content
    Then the app theme uses NoActionBar
    And screens pad with WindowInsets instead of a DarkActionBar strip

  Scenario: Obsolete XML screen layouts are removed
    Given Home, vehicle list/edit, trip stops, add stop, and blocked route are Compose
    Then activity and item row layout XML for those screens are deleted
