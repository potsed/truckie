Feature: Toll preferences stub
  As a driver
  I want toll avoid/allow preferences
  So that routing can respect my toll policy later

  @ff_tolls
  Scenario: Default toll preference is allow some
    Given feature flag "ff_tolls" is enabled
    When I open toll preferences
    Then the preference mode is "ALLOW_SOME"
