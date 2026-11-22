package com.truckie1.app.trips

/**
 * Hook for a future **verified address autocomplete** provider (map/search).
 *
 * Daniel requirement: stop addresses must be verified via autocomplete.
 * This nav-chrome PR only reserves the contract — do not invent a provider here.
 */
fun interface AddressAutocomplete {
    /**
     * @param query free-text typed in Add Stop
     * @return suggestions that are provider-verified (empty until a real provider is wired)
     */
    fun suggest(query: String): List<AddressSuggestion>
}

/**
 * Verified place candidate for a trip stop. Coordinates stay optional until a provider ships.
 */
data class AddressSuggestion(
    val label: String,
    val placeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** No-op until a real autocomplete backend is feature-flagged in. */
object NoOpAddressAutocomplete : AddressAutocomplete {
    override fun suggest(query: String): List<AddressSuggestion> = emptyList()
}
