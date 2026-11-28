Feature: Play hardening — backup policy and Settings disclaimer
  As a single-owner-operator preparing Truckie for Play / select-user release
  I want vehicle PII kept out of careless cloud backup and a clear Settings disclaimer
  So that rego, dimensions, and routes are not uploaded by Auto Backup, and drivers
  understand packs are guidance — roadside signs and NHVR rules still win

  # ATNA-96 under Post-review debt cleanup (ATNA-91).
  # Backup: dataExtractionRules / fullBackupContent exclude Room DB + settings prefs.
  # Disclaimer: ff_settings_disclaimer (defaults on) — not a permit; signs override packs;
  # on-board / realtime weight is guidance only.

  Scenario: Cloud backup excludes vehicle PII stores
    Given the Play backup policy for Truckie
    Then cloud backup does not include the Room database truckie_vehicles.db
    And cloud backup does not include shared preferences truckie_settings.xml
    And the manifest wires dataExtractionRules and fullBackupContent to those rules
    And allowBackup remains enabled only with those excludes (no open cloud dump of PII)

  Scenario: Settings disclaimer is feature-flagged and defaults on
    Given a fresh production start
    Then ff_settings_disclaimer is on by default
    And the Settings disclaimer section is shown
    When ff_settings_disclaimer is turned off
    Then the Settings disclaimer section is hidden

  Scenario: Disclaimer covers roadside signs, NHVR, and guidance-only weight
    Given ff_settings_disclaimer is on
    And the Settings screen is open
    Then the disclaimer states Truckie is not a permit
    And the disclaimer states physical roadside signs override digital packs
    And the disclaimer states drivers must verify with NHVR or road authorities
    And the disclaimer states on-board weight guidance is not a hard lock
    And the previous stub note about disclaimer landing later is gone
