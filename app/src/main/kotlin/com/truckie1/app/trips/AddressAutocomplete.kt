package com.truckie1.app.trips

/**
 * Pluggable **verified address autocomplete** provider (map/search).
 *
 * Daniel requirement: stop addresses must be verified via autocomplete —
 * no free-text-only unverified addresses when [VerifiedAddressCatalog] is on.
 *
 * [suggest] is **suspend** (ATNA-88): live Nominatim hops off the Compose
 * main thread. Providers must not be invoked from Compose `remember`.
 */
fun interface AddressAutocomplete {
    /**
     * @param query free-text typed in Add / Find stops
     * @return provider-verified suggestions (formatted label + coordinates)
     */
    suspend fun suggest(query: String): List<AddressSuggestion>
}

/**
 * Verified place candidate for a route stop.
 * [isVerified] is true only when both coordinates are present.
 */
data class AddressSuggestion(
    val label: String,
    val placeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val isVerified: Boolean
        get() = label.isNotBlank() && latitude != null && longitude != null
}

/** No-op when the verified-address flag is off or no provider is wired. */
object NoOpAddressAutocomplete : AddressAutocomplete {
    override suspend fun suggest(query: String): List<AddressSuggestion> = emptyList()
}

/**
 * Minimal HTTP GET for geocoding providers (injectable for unit tests).
 */
fun interface AddressHttpClient {
    fun get(url: String, userAgent: String): String
}
