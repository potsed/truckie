package com.truckie1.app.trips

/**
 * Composes Nominatim + optional G-NAF for **premise-precise** verified stops.
 *
 * Policy (ATNA-108):
 * 1. Ask Nominatim with number-keeping / structured variants; prefer housenumber hits.
 * 2. When [gnaf] is wired and the typed query has a unit/house premise, merge G-NAF
 *    hits and **prefer unit/house G-NAF points** over Nominatim street centroids.
 * 3. Never invent coordinates.
 */
class PremiseGeocodeAutocomplete(
    private val nominatim: AddressAutocomplete,
    private val gnaf: GnafAddressIndex? = null,
) : AddressAutocomplete {

    override suspend fun suggest(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val premise = AddressPremiseParser.parse(trimmed)
        val nominatimHits = nominatim.suggest(trimmed)
        val gnafHits = if (gnaf != null && premise.hasPremise) {
            gnaf.suggest(trimmed)
        } else {
            emptyList()
        }
        return merge(premise, nominatimHits, gnafHits)
    }

    internal fun merge(
        premise: AddressPremiseParser.Premise,
        nominatimHits: List<AddressSuggestion>,
        gnafHits: List<AddressSuggestion>,
    ): List<AddressSuggestion> {
        if (gnafHits.isEmpty()) return nominatimHits
        if (!premise.hasPremise) {
            return (nominatimHits + gnafHits).distinctBy { it.placeId to it.label }
        }
        val gnafUnit = gnafHits.filter { it.precision == AddressPrecision.UNIT }
        val gnafHouse = gnafHits.filter { it.precision == AddressPrecision.HOUSE }
        val nomPrecise = nominatimHits.filter { it.isPremisePrecise }
        val nomStreet = nominatimHits.filter { !it.isPremisePrecise }

        val ordered = mutableListOf<AddressSuggestion>()
        if (premise.unit != null) {
            ordered += gnafUnit
            ordered += gnafHouse
            ordered += nomPrecise
        } else {
            ordered += gnafHouse
            ordered += nomPrecise
            ordered += gnafUnit
        }
        // Street centroids only if nothing premise-precise exists.
        if (ordered.none { it.isPremisePrecise }) {
            ordered += nomStreet
        }
        return ordered.distinctBy { "${it.source}:${it.placeId}:${it.latitude}:${it.longitude}" }
    }
}
