package com.truckie1.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/licenses_page.feature — inventory SoT coverage (ATNA-116).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LicenseInventoryTest {

    @Test
    fun loadsCuratedStackFromAssetSot() {
        val inventory = LicenseInventory.loadFromAssets(RuntimeEnvironment.getApplication())
        assertTrue(inventory.entries.isNotEmpty())
        assertTrue(inventory.version >= 1)

        val blob = inventory.entries.joinToString("\n") { entry ->
            listOf(
                entry.id,
                entry.name,
                entry.license,
                entry.copyright.orEmpty(),
                entry.notes.orEmpty(),
                entry.url.orEmpty(),
            ).joinToString(" ")
        }.lowercase()

        assertTrue(blob.contains("maplibre"))
        assertTrue(blob.contains("openfreemap") || blob.contains("open free map"))
        assertTrue(blob.contains("openstreetmap") || blob.contains("osm"))
        assertTrue(blob.contains("valhalla"))
        assertTrue(
            blob.contains("interim") || blob.contains("hosted") || blob.contains("self-host"),
        )
        assertTrue(blob.contains("oci") || blob.contains("valhalla-scripted") || blob.contains("caddy"))
        assertTrue(blob.contains("g-naf") || blob.contains("gnaf"))
        assertTrue(blob.contains("cc-by") || blob.contains("cc by"))
        assertTrue(blob.contains("eula") || blob.contains("bulk mail"))
        assertTrue(blob.contains("androidx") || blob.contains("compose") || blob.contains("room"))
        assertTrue(blob.contains("apache") || blob.contains("bsd") || blob.contains("mit"))
    }

    @Test
    fun everyEntryHasNameAndLicense() {
        val inventory = LicenseInventory.loadFromAssets(RuntimeEnvironment.getApplication())
        inventory.entries.forEach { entry ->
            assertTrue(entry.id.isNotBlank())
            assertTrue(entry.name.isNotBlank())
            assertTrue(entry.license.isNotBlank())
        }
        assertFalse(inventory.entries.any { it.name.isBlank() || it.license.isBlank() })
    }

    @Test
    fun openStreetMapEntryRequiresAttribution() {
        val inventory = LicenseInventory.loadFromAssets(RuntimeEnvironment.getApplication())
        val osm = inventory.entries.first {
            it.id.contains("openstreetmap") ||
                it.name.contains("OpenStreetMap", ignoreCase = true) ||
                it.id.contains("openfreemap")
        }
        assertTrue(osm.attributionRequired)
        val text = listOf(osm.name, osm.notes.orEmpty(), osm.copyright.orEmpty())
            .joinToString(" ")
            .lowercase()
        assertTrue(text.contains("openstreetmap") || text.contains("osm"))
    }

    @Test
    fun parseJsonRoundTripShape() {
        val raw = """
            {
              "version": 1,
              "entries": [
                {
                  "id": "sample",
                  "name": "Sample Lib",
                  "license": "MIT",
                  "copyright": "Copyright 2026 Sample",
                  "url": "https://example.com",
                  "notes": "Curated note",
                  "attributionRequired": true
                }
              ]
            }
        """.trimIndent()
        val parsed = LicenseInventory.parseJson(raw)
        assertTrue(parsed.version == 1)
        assertTrue(parsed.entries.size == 1)
        val entry = parsed.entries.single()
        assertTrue(entry.id == "sample")
        assertTrue(entry.attributionRequired)
        assertTrue(entry.url == "https://example.com")
    }
}
