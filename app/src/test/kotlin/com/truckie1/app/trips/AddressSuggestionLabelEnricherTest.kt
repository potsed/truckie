package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/address_autocomplete_housenumber.feature — label enrich.
 */
class AddressSuggestionLabelEnricherTest {

    private val streetOnly = AddressSuggestion(
        label = "Elsie Street, Brisbane, Queensland, 4503, Australia",
        placeId = "27935006",
        latitude = -27.2543388,
        longitude = 152.9958419,
    )

    @Test
    fun prependsUnitAndHouseWhenNominatimLacksHousenumber() {
        val premise = AddressPremiseParser.parse("U21 1-31 Elsie Street Kallangur")
        val enriched = AddressSuggestionLabelEnricher.enrich(
            premise = premise,
            suggestions = listOf(streetOnly),
        )
        assertEquals(1, enriched.size)
        assertTrue(enriched[0].label.contains("U21"))
        assertTrue(enriched[0].label.contains("1-31"))
        assertTrue(enriched[0].label.contains("Elsie Street"))
        assertEquals(streetOnly.latitude, enriched[0].latitude)
        assertEquals(streetOnly.longitude, enriched[0].longitude)
        assertEquals(streetOnly.placeId, enriched[0].placeId)
        assertTrue(enriched[0].isVerified)
    }

    @Test
    fun doesNotDuplicateHouseNumberAlreadyInDisplayName() {
        val houseLevel = AddressSuggestion(
            label = "1 Queen Street, Brisbane City, Queensland, 4000, Australia",
            placeId = "240109959",
            latitude = -27.4704528,
            longitude = 153.0260339,
        )
        val premise = AddressPremiseParser.parse("1 Queen Street Brisbane")
        val enriched = AddressSuggestionLabelEnricher.enrich(
            premise = premise,
            suggestions = listOf(houseLevel),
        )
        assertEquals(houseLevel.label, enriched.single().label)
    }

    @Test
    fun leavesStreetOnlyQueryUnchanged() {
        val premise = AddressPremiseParser.parse("Elsie Street Kallangur")
        val enriched = AddressSuggestionLabelEnricher.enrich(
            premise = premise,
            suggestions = listOf(streetOnly),
        )
        assertEquals(streetOnly.label, enriched.single().label)
    }
}
