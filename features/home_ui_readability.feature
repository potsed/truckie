Feature: Home UI readability
  As a driver
  I want high-contrast Home chrome and controls
  So that status text and primary actions are readable

  # Bugfix for ActionBar/status-bar gap and low-contrast Material buttons.
  # Compose Material3 theme + edge-to-edge replaces DarkActionBar; flags unchanged.

  Scenario: Primary buttons use high-contrast label on filled navy
    Given the Truckie light theme colors are defined
    Then button fill "truckie_navy" and label "truckie_on_primary" meet WCAG AA contrast
    And body text "truckie_navy" on background "truckie_sky" meets WCAG AA contrast

  Scenario: Theme uses NoActionBar with edge-to-edge insets
    Given the Truckie light theme is applied
    Then the window theme is NoActionBar
    And primary button labels use the on-primary color
