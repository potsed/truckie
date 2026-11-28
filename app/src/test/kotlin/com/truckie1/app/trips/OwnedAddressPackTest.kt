package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/owned_au_address_pack.feature — owned AU address SoT scaffold.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OwnedAddressPackTest {

    @Test
    fun flagKeyAndBootstrapManifest() {
        assertEquals("ff_owned_address_pack", FlagKeys.OWNED_ADDRESS_PACK)
        assertEquals("ff_owned_address_pack", OwnedAddressPack.FLAG_KEY)
        assertEquals("0.0.0-sample", OwnedAddressPack.BOOTSTRAP_VERSION)
        assertEquals("addresses/au_sample_addresses.json", OwnedAddressPack.ASSET_PATH)
        val manifest = OwnedAddressPack.bootstrapManifest()
        assertEquals("AU", manifest.region)
        assertEquals("0.0.0-sample", manifest.version)
        assertEquals(0, manifest.recordCount)
        assertEquals(AddressPackManifest.SCHEMA_VERSION, manifest.schemaVersion)
        assertNull(manifest.downloadUrl)
        assertTrue(manifest.source.url.contains("data.gov.au"))
    }

    @Test
    fun packNotConsultedWhenFlagOff() {
        val flags = LocalFeatureFlags()
        assertFalse(OwnedAddressPack.isEnabled(flags))
        assertFalse(OwnedAddressPack.shouldConsultPack(flags))
    }

    @Test
    fun packConsultedWhenFlagOn() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.OWNED_ADDRESS_PACK to true))
        assertTrue(OwnedAddressPack.isEnabled(flags))
        assertTrue(OwnedAddressPack.shouldConsultPack(flags))
    }

    @Test
    fun emptyBootstrapAssetParsesWithoutInventingRows() {
        val context = RuntimeEnvironment.getApplication()
        val json = context.assets.open(OwnedAddressPack.ASSET_PATH).bufferedReader().use { it.readText() }
        val pack = AddressPackJson.parse(json)
        assertEquals("AU", pack.manifest.region)
        assertEquals("0.0.0-sample", pack.manifest.version)
        assertEquals(0, pack.manifest.recordCount)
        assertTrue(pack.addresses.isEmpty())
        assertEquals(AddressPackSource.BUNDLED_SAMPLE, pack.source)
    }

    @Test
    fun parsesRequiredRowFieldsWithoutInventingUnit() {
        val json = """
            {
              "manifest": {
                "region": "QLD",
                "version": "0.1.0-sample",
                "generatedAt": "2026-11-28",
                "schemaVersion": 1,
                "recordCount": 1,
                "source": {
                  "name": "Geoscape G-NAF",
                  "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
                  "retrievedAt": "2026-11-28"
                }
              },
              "addresses": [
                {
                  "id": "GAQLD163067541",
                  "lat": -27.25532783,
                  "lon": 152.99620218,
                  "formatted": "U21 1-31 Elsie Street Kallangur QLD 4503",
                  "unit": "21",
                  "number": "1-31",
                  "street": "Elsie Street",
                  "locality": "Kallangur",
                  "state": "QLD",
                  "postcode": "4503",
                  "precision": "unit",
                  "gnafPid": "GAQLD163067541",
                  "source": {
                    "name": "Geoscape G-NAF",
                    "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
                    "retrievedAt": "2026-11-28"
                  }
                }
              ]
            }
        """.trimIndent()
        val pack = AddressPackJson.parse(json)
        assertEquals(1, pack.addresses.size)
        val row = pack.addresses.single()
        assertEquals("GAQLD163067541", row.id)
        assertEquals(-27.25532783, row.lat, 1e-8)
        assertEquals(152.99620218, row.lon, 1e-8)
        assertEquals("U21 1-31 Elsie Street Kallangur QLD 4503", row.formatted)
        assertEquals("21", row.unit)
        assertEquals("1-31", row.number)
        assertEquals("Elsie Street", row.street)
        assertEquals("Kallangur", row.locality)
        assertEquals("QLD", row.state)
        assertEquals("4503", row.postcode)
        assertEquals(AddressPrecisionLevel.UNIT, row.precision)
        assertTrue(row.isPremisePrecise)
        assertEquals("Geoscape G-NAF", row.source.name)
    }

    @Test
    fun houseRowOmitsUnitWithoutInventing() {
        val json = """
            {
              "manifest": {
                "region": "QLD",
                "version": "0.1.0-sample",
                "generatedAt": "2026-11-28",
                "recordCount": 1,
                "source": {
                  "name": "Geoscape G-NAF",
                  "url": "https://example.test/gnaf",
                  "retrievedAt": "2026-11-28"
                }
              },
              "addresses": [
                {
                  "id": "GAQLD000000001",
                  "lat": -27.5,
                  "lon": 153.0,
                  "formatted": "10 Example Road Brisbane QLD 4000",
                  "number": "10",
                  "street": "Example Road",
                  "locality": "Brisbane",
                  "state": "QLD",
                  "postcode": "4000",
                  "precision": "house",
                  "source": {
                    "name": "Geoscape G-NAF",
                    "url": "https://example.test/gnaf",
                    "retrievedAt": "2026-11-28"
                  }
                }
              ]
            }
        """.trimIndent()
        val row = AddressPackJson.parse(json).addresses.single()
        assertNull(row.unit)
        assertEquals(AddressPrecisionLevel.HOUSE, row.precision)
        assertTrue(row.isPremisePrecise)
    }
}
