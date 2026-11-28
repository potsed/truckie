package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/owned_au_address_pack.feature — owned pack SoT over Nominatim.
 */
class OwnedAddressAutocompleteTest {

    private val streetCentroid = AddressSuggestion(
        label = "Elsie Street, Brisbane, Queensland, 4503, Australia",
        placeId = "street",
        latitude = -27.2543388,
        longitude = 152.9958419,
        precision = AddressPrecision.STREET,
        source = "nominatim",
    )

    private val packIndex = AddressPackIndex(
        listOf(
            AddressPackRecord(
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
                source = AddressSource(
                    name = "Geoscape G-NAF",
                    url = "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
                    retrievedAt = "2026-11-28",
                ),
                unit = "21",
                gnafPid = "GAQLD163067541",
            ),
        ),
    )

    @Test
    fun packHitBeatsNominatimStreetCentroid() = runBlocking {
        val nominatim = AddressAutocomplete { listOf(streetCentroid) }
        val autocomplete = OwnedAddressAutocomplete(
            nominatim = nominatim,
            pack = packIndex,
        )
        val suggestions = autocomplete.suggest("U21 1-31 Elsie Street Kallangur")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("GAQLD163067541", suggestions.first().placeId)
        assertEquals(-27.25532783, suggestions.first().latitude!!, 1e-8)
        assertEquals(152.99620218, suggestions.first().longitude!!, 1e-8)
        assertEquals(AddressPrecision.UNIT, suggestions.first().precision)
        assertTrue(suggestions.none { it.precision == AddressPrecision.STREET })
    }

    @Test
    fun nominatimTypeaheadOnlyWhenPackMisses() = runBlocking {
        val nominatim = AddressAutocomplete { listOf(streetCentroid) }
        val autocomplete = OwnedAddressAutocomplete(
            nominatim = nominatim,
            pack = AddressPackIndex(emptyList()),
        )
        val suggestions = autocomplete.suggest("Some Unknown Street Brisbane")
        assertEquals(1, suggestions.size)
        assertEquals("nominatim", suggestions.first().source)
    }

    @Test
    fun catalogUsesOwnedPackWhenFlagOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.ASYNC_ROUTING_IO to true,
                FlagKeys.OWNED_ADDRESS_PACK to true,
            ),
        )
        val autocomplete = VerifiedAddressCatalog.autocompleteFor(
            flags = flags,
            addressPackIndex = packIndex,
        )
        assertTrue(autocomplete is OwnedAddressAutocomplete)
    }

    @Test
    fun catalogIgnoresOwnedPackWhenFlagOff() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.ASYNC_ROUTING_IO to true,
                FlagKeys.OWNED_ADDRESS_PACK to false,
                FlagKeys.GNAF_ADDRESS_GEOCODE to false,
            ),
        )
        val autocomplete = VerifiedAddressCatalog.autocompleteFor(
            flags = flags,
            addressPackIndex = packIndex,
        )
        assertTrue(autocomplete is NominatimAddressAutocomplete)
    }
}
