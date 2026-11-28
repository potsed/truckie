package com.truckie1.app.play

import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.NavigationAppPreference

/**
 * Play / Auto Backup policy for vehicle and driver PII (ATNA-96).
 *
 * Room stores rego, dims, mass ratings, and routes; settings prefs hold nav
 * handoff choice. Neither may enter cloud backup casually (MASVS storage).
 *
 * Manifest wires [DATA_EXTRACTION_RULES_REF] (API 31+) and
 * [FULL_BACKUP_CONTENT_REF] (legacy) to XML that excludes these paths.
 * [allowBackupWithExcludes] stays true so the platform still honours the
 * exclude lists — not an open dump of the app sandbox.
 *
 * Gherkin: features/play_hardening_backup_disclaimer.feature
 */
object PlayBackupPolicy {
    const val VEHICLE_DATABASE_FILE: String = VehicleDatabase.DATABASE_NAME
    const val SETTINGS_PREFS_FILE: String = "${NavigationAppPreference.PREFS_NAME}.xml"

    const val DATA_EXTRACTION_RULES_REF: String = "@xml/data_extraction_rules"
    const val FULL_BACKUP_CONTENT_REF: String = "@xml/backup_rules"

    /** Manifest keeps allowBackup; cloud backup is narrowed by the XML excludes. */
    const val allowBackupWithExcludes: Boolean = true
    const val excludesVehicleDatabaseFromCloudBackup: Boolean = true
    const val excludesSettingsPrefsFromCloudBackup: Boolean = true
    const val carelessCloudBackupOfVehiclePiiAllowed: Boolean = false
}
