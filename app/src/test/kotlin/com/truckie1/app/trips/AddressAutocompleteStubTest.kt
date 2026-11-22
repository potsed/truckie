package com.truckie1.app.trips

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reserves the verified-autocomplete contract without shipping a provider (nav-chrome PR).
 */
class AddressAutocompleteStubTest {

    @Test
    fun noOpReturnsNoSuggestions() {
        assertTrue(NoOpAddressAutocomplete.suggest("Brisbane").isEmpty())
        assertTrue(NoOpAddressAutocomplete.suggest("").isEmpty())
    }
}
