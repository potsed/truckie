package com.truckie1.app.trips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NoOp provider used when ff_address_autocomplete is off.
 */
class AddressAutocompleteStubTest {

    @Test
    fun noOpReturnsNoSuggestions() {
        assertTrue(NoOpAddressAutocomplete.suggest("Brisbane").isEmpty())
        assertTrue(NoOpAddressAutocomplete.suggest("").isEmpty())
    }

    @Test
    fun suggestionWithoutCoordsIsNotVerified() {
        val incomplete = AddressSuggestion(label = "Somewhere")
        assertFalse(incomplete.isVerified)
    }

    @Test
    fun suggestionWithCoordsIsVerified() {
        val verified = AddressSuggestion(
            label = "1 Queen St, Brisbane",
            latitude = -27.47,
            longitude = 153.02,
        )
        assertTrue(verified.isVerified)
    }
}
