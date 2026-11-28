package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/address_autocomplete_housenumber.feature — G-NAF unit point.
 */
class GnafAddressIndexTest {

    private val packJson = """
        {
          "region": "QLD",
          "version": "0.1.0-test",
          "addresses": [
            {
              "gnafPid": "GAQLD163067541",
              "houseNumber": "1-31",
              "street": "Elsie Street",
              "unit": "UNIT 21",
              "longitude": 152.99620218,
              "latitude": -27.25532783,
              "locality": "Kallangur",
              "postcode": "4503",
              "state": "QLD",
              "accuracy": 2
            },
            {
              "gnafPid": "GAQLD163072614",
              "houseNumber": "1-31",
              "street": "Elsie Street",
              "unit": null,
              "longitude": 152.99568248,
              "latitude": -27.25542385,
              "locality": "Kallangur",
              "postcode": "4503",
              "state": "QLD",
              "accuracy": 2
            }
          ]
        }
    """.trimIndent()

    @Test
    fun resolvesUnit21ToGnafCoordinates() {
        val index = GnafAddressIndex.parsePackJson(packJson)
        val suggestions = index.suggest("U21 1-31 Elsie Street Kallangur")
        assertTrue(suggestions.isNotEmpty())
        val top = suggestions.first()
        assertEquals(AddressPrecision.UNIT, top.precision)
        assertEquals(GnafAddressIndex.SOURCE_ID, top.source)
        assertEquals(-27.25532783, top.latitude!!, 1e-7)
        assertEquals(152.99620218, top.longitude!!, 1e-7)
        assertTrue(top.label.contains("21"))
        assertTrue(top.label.contains("1-31"))
        assertTrue(top.label.contains("Elsie", ignoreCase = true))
        assertTrue(top.isPremisePrecise)
    }

    @Test
    fun houseOnlyQueryReturnsHouseLevelGnafPoint() {
        val index = GnafAddressIndex.parsePackJson(packJson)
        val suggestions = index.suggest("1-31 Elsie Street Kallangur")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.precision == AddressPrecision.HOUSE || it.precision == AddressPrecision.UNIT })
        assertTrue(suggestions.all { it.source == GnafAddressIndex.SOURCE_ID })
    }
}
