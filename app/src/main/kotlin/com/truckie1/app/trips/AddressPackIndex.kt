package com.truckie1.app.trips

/**
 * In-memory search over an owned G-NAF [AddressPack] (ATNA-111).
 *
 * One G-NAF-backed search path — never invents coordinates.
 * Gherkin: features/owned_au_address_pack.feature
 */
class AddressPackIndex(
    private val addresses: List<AddressPackRecord>,
) {
    fun suggest(query: String, limit: Int = DEFAULT_LIMIT): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || addresses.isEmpty()) return emptyList()
        val premise = AddressPremiseParser.parse(trimmed)
        return addresses.mapNotNull { addr ->
            val score = score(addr, premise, trimmed)
            if (score <= 0) null else score to addr
        }.sortedByDescending { it.first }
            .take(limit)
            .map { (_, addr) -> addr.toSuggestion() }
    }

    private fun score(
        addr: AddressPackRecord,
        premise: AddressPremiseParser.Premise,
        rawQuery: String,
    ): Int {
        var score = 0
        val q = rawQuery.lowercase()
        val street = addr.street.lowercase()
        val locality = addr.locality.lowercase()
        if (street.isNotBlank() && q.contains(street.substringBefore(" ").lowercase())) {
            score += 5
        }
        premise.streetName?.let { sn ->
            if (street.contains(sn.lowercase().substringBefore(" "))) score += 15
        }
        premise.locality?.let { loc ->
            if (locality == loc.lowercase()) score += 20
        } ?: run {
            if (q.contains(locality)) score += 10
        }
        premise.houseNumber?.let { house ->
            if (addr.number.equals(house, ignoreCase = true)) score += 40
            else return 0
        }
        premise.unit?.let { unit ->
            val digits = unitDigits(unit)
            val addrDigits = unitDigits(addr.unit.orEmpty())
            when {
                digits.isNotEmpty() && digits == addrDigits -> score += 50
                addr.unit.isNullOrBlank() -> score += 0
                else -> return 0
            }
        }
        if (premise.unit != null && addr.unit.isNullOrBlank()) {
            score += 5
        }
        if (premise.unit == null && !addr.unit.isNullOrBlank()) {
            score -= 5
        }
        return score
    }

    private fun unitDigits(raw: String): String =
        Regex("""(?i)(?:unit\s*|u)?(\d+[A-Za-z]?)""").find(raw)?.groupValues?.get(1).orEmpty()

    companion object {
        const val DEFAULT_LIMIT = 5
        const val SOURCE_ID = "gnaf"

        fun fromPack(pack: AddressPack): AddressPackIndex =
            AddressPackIndex(pack.addresses.filter { it.isPremisePrecise })
    }
}

fun AddressPackRecord.toSuggestion(): AddressSuggestion =
    AddressSuggestion(
        label = formatted,
        placeId = id,
        latitude = lat,
        longitude = lon,
        precision = when (precision) {
            AddressPrecisionLevel.UNIT -> AddressPrecision.UNIT
            AddressPrecisionLevel.HOUSE -> AddressPrecision.HOUSE
            AddressPrecisionLevel.BUILDING -> AddressPrecision.BUILDING
            AddressPrecisionLevel.STREET -> AddressPrecision.STREET
        },
        source = AddressPackIndex.SOURCE_ID,
    )
