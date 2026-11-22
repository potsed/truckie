package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Catalog for verified address autocomplete (`ff_address_autocomplete`).
 */
object VerifiedAddressCatalog {
    const val FLAG: String = FlagKeys.ADDRESS_AUTOCOMPLETE
    const val DEFAULT_PROVIDER_ID: String = "nominatim"
    const val DEFAULT_COUNTRY_CODES: String = NominatimAddressAutocomplete.DEFAULT_COUNTRY_CODES

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE)

    fun allowsFreeTextAddress(flags: FeatureFlags): Boolean = !isEnabled(flags)

    fun requiresVerifiedSuggestion(flags: FeatureFlags): Boolean = isEnabled(flags)

    /** Default Nominatim AU provider (real HTTP). Inject a fake client in tests. */
    fun defaultAutocomplete(
        client: AddressHttpClient = DefaultAddressHttpClient,
    ): AddressAutocomplete = NominatimAddressAutocomplete(
        client = client,
        countryCodes = DEFAULT_COUNTRY_CODES,
    )

    /**
     * Provider for the map-home sheet: Nominatim when the flag is on, otherwise NoOp.
     */
    fun autocompleteFor(flags: FeatureFlags): AddressAutocomplete =
        if (isEnabled(flags)) defaultAutocomplete() else NoOpAddressAutocomplete
}
