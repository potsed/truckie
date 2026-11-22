Feature: Home UI readability
  As a driver
  I want high-contrast Home chrome and controls
  So that status text and primary actions are readable

  # Bugfix for ActionBar/status-bar gap and low-contrast Material buttons.
  # Theme/colors apply app-wide; feature flags and navigation behavior stay unchanged.

  Scenario: Primary buttons use high-contrast label on filled navy
    Given the Truckie1 light theme colors are defined
    Then button fill "truckie_navy" and label "truckie_on_primary" meet WCAG AA contrast
    And body text "truckie_navy" on background "truckie_sky" meets WCAG AA contrast

  Scenario: Theme keeps ActionBar flush with the status bar
    Given the Truckie1 light theme is applied
    Then status bar color matches the primary ActionBar color
    And primary button labels use the on-primary color
