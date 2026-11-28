package com.truckie1.app.trips

/**
 * G-NAF owned-pack SoT autocomplete (ATNA-111).
 *
 * When the pack covers the query, verified suggestions come from G-NAF only.
 * Nominatim may supply typeahead labels while the driver types, but must not
 * supply Navigate coordinates once a pack hit resolves.
 *
 * Gherkin: features/owned_au_address_pack.feature
 */
class OwnedAddressAutocomplete(
    private val nominatim: AddressAutocomplete,
    private val pack: AddressPackIndex,
) : AddressAutocomplete {

    override suspend fun suggest(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val packHits = pack.suggest(trimmed)
        if (packHits.isNotEmpty()) {
            // Pack is SoT — do not mix Nominatim street centroids into the hit list.
            return packHits
        }
        // Typeahead-only until a G-NAF pack hit resolves.
        return nominatim.suggest(trimmed)
    }
}
