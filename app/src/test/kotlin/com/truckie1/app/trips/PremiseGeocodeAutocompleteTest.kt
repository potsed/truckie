package com.truckie1.app.trips

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/address_autocomplete_housenumber.feature — compose Nominatim + G-NAF.
 */
class PremiseGeocodeAutocompleteTest {

    private val streetCentroid = AddressSuggestion(
        label = "Elsie Street, Brisbane, Queensland, 4503, Australia",
        placeId = "street",
        latitude = -27.2543388,
        longitude = 152.9958419,
        precision = AddressPrecision.STREET,
        source = "nominatim",
    )

    private val gnafUnit21 = AddressSuggestion(
        label = "UNIT 21 1-31 Elsie Street, Kallangur QLD 4503",
        placeId = "GAQLD163067541",
        latitude = -27.25532783,
        longitude = 152.99620218,
        precision = AddressPrecision.UNIT,
        source = "gnaf",
    )

    @Test
    fun prefersGnafUnitOverNominatimStreetCentroid() = runBlocking {
        val nominatim = AddressAutocomplete { listOf(streetCentroid) }
        val gnaf = GnafAddressIndex(
            listOf(
                GnafAddress(
                    gnafPid = "GAQLD163067541",
                    houseNumber = "1-31",
                    street = "Elsie Street",
                    unit = "UNIT 21",
                    latitude = -27.25532783,
                    longitude = 152.99620218,
                    locality = "Kallangur",
                    postcode = "4503",
                    state = "QLD",
                ),
            ),
        )
        val autocomplete = PremiseGeocodeAutocomplete(nominatim = nominatim, gnaf = gnaf)
        val suggestions = autocomplete.suggest("U21 1-31 Elsie Street Kallangur")
        assertTrue(suggestions.isNotEmpty())
        assertEquals(AddressPrecision.UNIT, suggestions.first().precision)
        assertEquals(gnafUnit21.latitude!!, suggestions.first().latitude!!, 1e-7)
        assertEquals(gnafUnit21.longitude!!, suggestions.first().longitude!!, 1e-7)
        assertTrue(suggestions.none { it.precision == AddressPrecision.STREET })
    }

    @Test
    fun mergeKeepsNominatimPreciseWhenNoGnaf() {
        val building = AddressSuggestion(
            label = "Elsium Rise, 1-31, Kallangur, Queensland, 4503, Australia",
            placeId = "27624684",
            latitude = -27.2544437,
            longitude = 152.9963696,
            precision = AddressPrecision.BUILDING,
            source = "nominatim",
        )
        val premise = AddressPremiseParser.parse("1-31 Elsie Street Kallangur")
        val merged = PremiseGeocodeAutocomplete(nominatim = AddressAutocomplete { emptyList() })
            .merge(premise, listOf(building), emptyList())
        assertEquals(1, merged.size)
        assertEquals(AddressPrecision.BUILDING, merged[0].precision)
    }
}
