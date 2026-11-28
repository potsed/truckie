package com.truckie1.app.trips

import com.truckie1.app.routing.AsyncRoutingIoCatalog
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Catalog for verified address autocomplete (`ff_address_autocomplete`).
 *
 * Live Nominatim is only wired when [AsyncRoutingIoCatalog.allowsLiveNominatim]
 * (ATNA-88) — never sync-on-main HTTP when async I/O is off.
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
     * Provider for the map-home sheet: Nominatim when autocomplete **and** async
     * routing I/O are on; otherwise NoOp (never sync live HTTP).
     *
     * Prefer **one G-NAF path** when [OwnedAddressPack] is on and an
     * [addressPackIndex] is present ([OwnedAddressAutocomplete] — pack SoT,
     * Nominatim typeahead until pack hit). Else interim [GnafAddressCatalog]
     * + [PremiseGeocodeAutocomplete] when that flag and [gnafIndex] are set.
     */
    fun autocompleteFor(
        flags: FeatureFlags,
        gnafIndex: GnafAddressIndex? = null,
        addressPackIndex: AddressPackIndex? = null,
    ): AddressAutocomplete {
        if (!AsyncRoutingIoCatalog.allowsLiveNominatim(flags)) {
            return NoOpAddressAutocomplete
        }
        val nominatim = defaultAutocomplete()
        if (OwnedAddressPack.shouldConsultPack(flags) && addressPackIndex != null) {
            return OwnedAddressAutocomplete(
                nominatim = nominatim,
                pack = addressPackIndex,
            )
        }
        return if (GnafAddressCatalog.isEnabled(flags) && gnafIndex != null) {
            PremiseGeocodeAutocomplete(nominatim = nominatim, gnaf = gnafIndex)
        } else {
            nominatim
        }
    }
}
