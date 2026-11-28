package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/address_autocomplete_housenumber.feature — AU premise parse.
 */
class AddressPremiseParserTest {

    @Test
    fun parsesUnitShorthandAndHouseRange() {
        val premise = AddressPremiseParser.parse("U21 1-31 Elsie Street Kallangur")
        assertEquals("U21", premise.unit)
        assertEquals("1-31", premise.houseNumber)
        assertEquals("Elsie Street Kallangur", premise.remainder)
        assertEquals("Elsie Street", premise.streetName)
        assertEquals("Kallangur", premise.locality)
        assertTrue(premise.hasPremise)
    }

    @Test
    fun parsesUnitWordAndHouseNumber() {
        val premise = AddressPremiseParser.parse("Unit 5 42 Queen Street Brisbane")
        assertEquals("Unit 5", premise.unit)
        assertEquals("42", premise.houseNumber)
        assertEquals("Queen Street Brisbane", premise.remainder)
    }

    @Test
    fun parsesSlashUnitHouseForm() {
        val premise = AddressPremiseParser.parse("21/1-31 Elsie Street Kallangur")
        assertEquals("21", premise.unit)
        assertEquals("1-31", premise.houseNumber)
        assertEquals("Elsie Street Kallangur", premise.remainder)
    }

    @Test
    fun parsesSimpleHouseNumberWithoutUnit() {
        val premise = AddressPremiseParser.parse("42 Queen Street Brisbane")
        assertNull(premise.unit)
        assertEquals("42", premise.houseNumber)
        assertEquals("Queen Street Brisbane", premise.remainder)
        assertTrue(premise.hasPremise)
    }

    @Test
    fun parsesAlphanumericHouseNumber() {
        val premise = AddressPremiseParser.parse("1A Boronia Road Brisbane Airport")
        assertNull(premise.unit)
        assertEquals("1A", premise.houseNumber)
        assertEquals("Boronia Road Brisbane Airport", premise.remainder)
    }

    @Test
    fun streetOnlyHasNoPremise() {
        val premise = AddressPremiseParser.parse("Elsie Street Kallangur")
        assertNull(premise.unit)
        assertNull(premise.houseNumber)
        assertEquals("Elsie Street Kallangur", premise.remainder)
        assertFalse(premise.hasPremise)
    }
}
