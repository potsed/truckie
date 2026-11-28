package com.truckie1.app.play

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.truckie1.app.R
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.NavigationAppPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * Gherkin: features/play_hardening_backup_disclaimer.feature — backup / PII (ATNA-96).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayBackupPolicyTest {

    @Test
    fun policyExcludesVehicleDatabaseAndSettingsPrefs() {
        assertEquals("truckie_vehicles.db", PlayBackupPolicy.VEHICLE_DATABASE_FILE)
        assertEquals(VehicleDatabase.DATABASE_NAME, PlayBackupPolicy.VEHICLE_DATABASE_FILE)
        assertEquals("truckie_settings.xml", PlayBackupPolicy.SETTINGS_PREFS_FILE)
        assertEquals(
            "${NavigationAppPreference.PREFS_NAME}.xml",
            PlayBackupPolicy.SETTINGS_PREFS_FILE,
        )
        assertTrue(PlayBackupPolicy.excludesVehicleDatabaseFromCloudBackup)
        assertTrue(PlayBackupPolicy.excludesSettingsPrefsFromCloudBackup)
        assertFalse(PlayBackupPolicy.carelessCloudBackupOfVehiclePiiAllowed)
    }

    @Test
    fun manifestWiresExtractionAndLegacyBackupRules() {
        assertEquals("@xml/data_extraction_rules", PlayBackupPolicy.DATA_EXTRACTION_RULES_REF)
        assertEquals("@xml/backup_rules", PlayBackupPolicy.FULL_BACKUP_CONTENT_REF)
        assertTrue(PlayBackupPolicy.allowBackupWithExcludes)
    }

    @Test
    fun dataExtractionRulesXmlExcludesSensitiveDomains() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val excludes = parseCloudBackupExcludes(context, R.xml.data_extraction_rules)
        assertTrue(
            excludes.any {
                it.domain == "database" && it.path == PlayBackupPolicy.VEHICLE_DATABASE_FILE
            },
        )
        assertTrue(
            excludes.any {
                it.domain == "sharedpref" && it.path == PlayBackupPolicy.SETTINGS_PREFS_FILE
            },
        )
    }

    @Test
    fun legacyBackupRulesXmlExcludesSensitiveDomains() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val excludes = parseFullBackupExcludes(context, R.xml.backup_rules)
        assertTrue(
            excludes.any {
                it.domain == "database" && it.path == PlayBackupPolicy.VEHICLE_DATABASE_FILE
            },
        )
        assertTrue(
            excludes.any {
                it.domain == "sharedpref" && it.path == PlayBackupPolicy.SETTINGS_PREFS_FILE
            },
        )
    }

    private data class Exclude(val domain: String, val path: String)

    private fun parseCloudBackupExcludes(context: Context, resId: Int): List<Exclude> {
        val parser = context.resources.getXml(resId)
        val out = mutableListOf<Exclude>()
        var inCloudBackup = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "cloud-backup" -> inCloudBackup = true
                    "exclude" -> if (inCloudBackup) {
                        out += Exclude(
                            domain = parser.getAttributeValue(null, "domain")!!,
                            path = parser.getAttributeValue(null, "path")!!,
                        )
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "cloud-backup") {
                    inCloudBackup = false
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun parseFullBackupExcludes(context: Context, resId: Int): List<Exclude> {
        val parser = context.resources.getXml(resId)
        val out = mutableListOf<Exclude>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "exclude") {
                out += Exclude(
                    domain = parser.getAttributeValue(null, "domain")!!,
                    path = parser.getAttributeValue(null, "path")!!,
                )
            }
            event = parser.next()
        }
        return out
    }
}
