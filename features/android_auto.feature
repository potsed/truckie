Feature: Android Auto surfaces gated by ff_android_auto
  As a single-owner-operator driver using a car head unit
  I want Android Auto navigation behind a feature flag and API 23+
  So that Auto only ships when NavigationTemplate quality and next-destination UX are ready

  # Product toggle: ff_android_auto (ATNA-10 / Phase G)
  # Defaults OFF in production and debug — keep off until Auto compliance.
  # Gate: AndroidAutoGate requires ff_android_auto AND SDK_INT >= 23.
  # Scaffold today: TruckieCarAppService + MessageTemplate "Truckie navigation (scaffold)".
  # When Auto navigation lands: Cars App NavigationTemplate + clear next destination
  # on the car UI (first-class; see ff_next_destination_display / ATNA-83).
  # Phone: keep screen active while navigating; when Auto is connected the phone
  # may blank to save power (preference contract — implementation later).
  # Out of scope: Play Auto listing submission, enabling the flag in dogfood.

  Scenario: Android Auto flag defaults off
    Given a fresh LocalFeatureFlags instance
    Then ff_android_auto is off
    And Android Auto stays disabled even in debug dogfood defaults

  Scenario: Gate requires both the flag and API 23+
    Given ff_android_auto is enabled
    When the device API level is 22
    Then Android Auto is not enabled
    When the device API level is 23
    Then Android Auto is enabled
    Given ff_android_auto is off
    And the device API level is 30
    Then Android Auto is not enabled

  Scenario: Car session stays scaffold until NavigationTemplate ships
    Given ff_android_auto is enabled
    And the device API level is 23 or higher
    Then the Cars App entry uses a scaffold MessageTemplate
    And NavigationTemplate is not yet the shipped surface
    And the catalog requires NavigationTemplate before production Auto enablement
    And the next destination must show clearly on Auto when ATNA-10 lands

  Scenario: Phone screen-on and Auto blank are contracted
    Given ff_android_auto behaviour contracts
    Then navigating on phone keeps the screen active by default
    And when Android Auto is connected the phone may blank to save power
    And Auto host validation stays strict in release builds
