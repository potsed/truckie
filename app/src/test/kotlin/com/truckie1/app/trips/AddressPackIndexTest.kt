package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/owned_au_address_pack.feature — Kallangur U21 from owned pack.
 */
class AddressPackIndexTest {

    private val source = AddressSource(
        name = "Geoscape G-NAF",
        url = "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
        retrievedAt = "2026-11-28",
    )

    private val unit21 = AddressPackRecord(
        id = "GAQLD163067541",
        lat = -27.25532783,
        lon = 152.99620218,
        formatted = "U21 1-31 Elsie Street Kallangur QLD 4503",
        number = "1-31",
        street = "Elsie Street",
        locality = "Kallangur",
        state = "QLD",
        postcode = "4503",
        precision = AddressPrecisionLevel.UNIT,
        source = source,
        unit = "21",
        gnafPid = "GAQLD163067541",
    )

    @Test
    fun kallangurU21ResolvesWithGnafCoords() {
        val index = AddressPackIndex(listOf(unit21))
        val hits = index.suggest("U21 1-31 Elsie Street Kallangur")
        assertTrue(hits.isNotEmpty())
        val top = hits.first()
        assertEquals("GAQLD163067541", top.placeId)
        assertEquals(-27.25532783, top.latitude!!, 1e-8)
        assertEquals(152.99620218, top.longitude!!, 1e-8)
        assertEquals(AddressPrecision.UNIT, top.precision)
        assertEquals(AddressPackIndex.SOURCE_ID, top.source)
        assertTrue(top.isVerified)
        assertTrue(top.isPremisePrecise)
    }

    @Test
    fun emptyQueryOrEmptyPackReturnsNothing() {
        assertTrue(AddressPackIndex(listOf(unit21)).suggest("").isEmpty())
        assertTrue(AddressPackIndex(emptyList()).suggest("U21 Elsie").isEmpty())
    }
}
