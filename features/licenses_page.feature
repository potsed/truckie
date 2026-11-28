Feature: Licenses page for third-party dependencies
  As a single-owner-operator driver (and Play / select-user reviewer)
  I want a Settings → Licenses page listing what Truckie uses and under which licences
  So that OSS / free-commercial attribution stays accurate and maintainable

  # ATNA-116. SoT is assets/licenses/third_party.json (UI renders from it);
  # docs/licenses.md mirrors the same inventory for humans.
  # Flag ff_licenses_page defaults on (legal / attribution surface, like disclaimer).

  Scenario: Licenses page is feature-flagged and defaults on
    Given a fresh production start
    Then ff_licenses_page is on by default
    And Settings shows a Licenses entry
    When ff_licenses_page is turned off
    Then the Licenses entry is hidden from Settings

  Scenario: Drivers can open Licenses from Settings
    Given ff_licenses_page is on
    And the Settings screen is open
    When the driver taps Licenses
    Then the Licenses page lists third-party entries from the inventory SoT
    And each entry shows a name and licence

  Scenario: Inventory covers the current stack
    Given the licenses inventory SoT
    Then it includes MapLibre Native Android
    And it includes OpenFreeMap with OpenStreetMap attribution
    And it includes Valhalla with interim hosted and self-host (OCI) notes
    And it includes the Valhalla scripted Docker image and Caddy as ops-only entries
    And it includes G-NAF with CC-BY and EULA constraints
    And it includes AndroidX / Compose / Room as practical curated entries

  Scenario: OpenStreetMap / OpenFreeMap attribution is visible to drivers
    Given map home or the Licenses page
    Then OpenStreetMap contributor attribution is available to drivers
    And MapLibre map attribution remains enabled (not stripped)
