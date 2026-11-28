package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: verified_address_autocomplete.feature — flag gate for verified stops.
 */
class VerifiedAddressCatalogTest {

    @Test
    fun flagKeyMatchesGherkin() {
        assertEquals("ff_address_autocomplete", FlagKeys.ADDRESS_AUTOCOMPLETE)
        assertEquals(FlagKeys.ADDRESS_AUTOCOMPLETE, VerifiedAddressCatalog.FLAG)
    }

    @Test
    fun defaultsOffAllowsFreeTextUntilEnabled() {
        val flags = LocalFeatureFlags()
        assertFalse(VerifiedAddressCatalog.isEnabled(flags))
        assertTrue(VerifiedAddressCatalog.allowsFreeTextAddress(flags))
        assertFalse(VerifiedAddressCatalog.requiresVerifiedSuggestion(flags))
    }

    @Test
    fun whenEnabledRequiresVerifiedSuggestion() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.ADDRESS_AUTOCOMPLETE to true))
        assertTrue(VerifiedAddressCatalog.isEnabled(flags))
        assertFalse(VerifiedAddressCatalog.allowsFreeTextAddress(flags))
        assertTrue(VerifiedAddressCatalog.requiresVerifiedSuggestion(flags))
    }

    @Test
    fun nominatimIsDefaultProviderWhenEnabled() {
        assertEquals("nominatim", VerifiedAddressCatalog.DEFAULT_PROVIDER_ID)
        assertEquals("au", VerifiedAddressCatalog.DEFAULT_COUNTRY_CODES)
        assertTrue(VerifiedAddressCatalog.defaultAutocomplete() is NominatimAddressAutocomplete)
    }

    @Test
    fun autocompleteForRequiresAsyncRoutingIo() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.ASYNC_ROUTING_IO to false,
            ),
        )
        assertTrue(VerifiedAddressCatalog.autocompleteFor(flags) is NoOpAddressAutocomplete)

        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        assertTrue(VerifiedAddressCatalog.autocompleteFor(flags) is NominatimAddressAutocomplete)
    }

    @Test
    fun autocompleteForWrapsNominatimWithGnafWhenFlagAndIndexPresent() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.ASYNC_ROUTING_IO to true,
                FlagKeys.GNAF_ADDRESS_GEOCODE to true,
            ),
        )
        val gnaf = GnafAddressIndex(emptyList())
        assertTrue(
            VerifiedAddressCatalog.autocompleteFor(flags, gnaf) is PremiseGeocodeAutocomplete,
        )
    }
}
