package com.truckie1.app.trips

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/address_autocomplete_housenumber.feature — number-keeping queries.
 */
class NominatimQueryPlannerTest {

    @Test
    fun includesNumberKeepingLocalityQuery() {
        val premise = AddressPremiseParser.parse("1-31 Elsie Street Kallangur")
        val queries = NominatimQueryPlanner.candidateQueries(
            "1-31 Elsie Street Kallangur",
            premise,
        )
        assertTrue(queries.any { it == "1-31, Kallangur" || it.startsWith("1-31,") })
        assertTrue(queries.any { it.contains("Elsie") })
    }

    @Test
    fun inferPrecisionMarksHighwayWithoutHouseAsStreet() {
        assertEqualsStreet(
            NominatimSearchParser.inferPrecision(null, "highway", "residential"),
        )
        assertTrue(
            NominatimSearchParser.inferPrecision("1-31", "landuse", "residential") ==
                AddressPrecision.BUILDING,
        )
    }

    private fun assertEqualsStreet(precision: AddressPrecision) {
        assertTrue(precision == AddressPrecision.STREET)
    }
}
