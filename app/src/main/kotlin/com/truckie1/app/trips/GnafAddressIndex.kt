package com.truckie1.app.trips

/**
 * In-memory G-NAF address points (CC-BY) for premise-level autocomplete.
 *
 * Pack JSON is a QLD-scoped (or smaller) extract — never invents coordinates.
 * Source: Geocoded National Address File via data.gov.au / OpenAddresses AU.
 */
class GnafAddressIndex(
    private val addresses: List<GnafAddress>,
) {
    fun suggest(query: String, limit: Int = DEFAULT_LIMIT): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || addresses.isEmpty()) return emptyList()
        val premise = AddressPremiseParser.parse(trimmed)
        val scored = addresses.mapNotNull { addr ->
            val score = score(addr, premise, trimmed)
            if (score <= 0) null else score to addr
        }.sortedByDescending { it.first }
            .take(limit)
        return scored.map { (_, addr) -> addr.toSuggestion() }
    }

    private fun score(
        addr: GnafAddress,
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
            if (addr.houseNumber.equals(house, ignoreCase = true)) score += 40
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
            // House-level G-NAF row when unit missing from pack match path.
            score += 5
        }
        if (premise.unit == null && !addr.unit.isNullOrBlank()) {
            // Prefer the base street-number point when the driver did not type a unit.
            score -= 5
        }
        return score
    }

    private fun unitDigits(raw: String): String =
        Regex("""(?i)(?:unit\s*)?(\d+[A-Za-z]?)""").find(raw)?.groupValues?.get(1).orEmpty()

    companion object {
        const val DEFAULT_LIMIT = 5
        const val SOURCE_ID = "gnaf"
        const val ASSET_PATH = "gnaf/qld_sample_addresses.json"

        fun parsePackJson(body: String): GnafAddressIndex {
            val addresses = GnafPackParser.parse(body)
            return GnafAddressIndex(addresses)
        }
    }
}

data class GnafAddress(
    val gnafPid: String,
    val houseNumber: String,
    val street: String,
    val unit: String?,
    val latitude: Double,
    val longitude: Double,
    val locality: String,
    val postcode: String?,
    val state: String?,
) {
    fun toSuggestion(): AddressSuggestion {
        val unitPart = unit?.takeIf { it.isNotBlank() }
        val label = buildString {
            if (unitPart != null) {
                append(unitPart)
                append(' ')
            }
            append(houseNumber)
            append(' ')
            append(street)
            append(", ")
            append(locality)
            if (!state.isNullOrBlank()) {
                append(' ')
                append(state)
            }
            if (!postcode.isNullOrBlank()) {
                append(' ')
                append(postcode)
            }
        }
        return AddressSuggestion(
            label = label,
            placeId = gnafPid,
            latitude = latitude,
            longitude = longitude,
            precision = if (!unitPart.isNullOrBlank()) {
                AddressPrecision.UNIT
            } else {
                AddressPrecision.HOUSE
            },
            source = GnafAddressIndex.SOURCE_ID,
        )
    }
}

/**
 * Minimal JSON pack parser for G-NAF sample extracts (no JSON library).
 */
object GnafPackParser {
    fun parse(body: String): List<GnafAddress> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        val arrayStart = trimmed.indexOf("\"addresses\"")
        if (arrayStart < 0) return emptyList()
        val bracket = trimmed.indexOf('[', arrayStart)
        if (bracket < 0) return emptyList()
        val results = mutableListOf<GnafAddress>()
        var index = bracket
        while (index < trimmed.length) {
            val objStart = trimmed.indexOf('{', index)
            if (objStart < 0) break
            val objEnd = NominatimSearchParser.endOfJsonObject(trimmed, objStart)
            if (objEnd < 0) break
            val obj = trimmed.substring(objStart, objEnd + 1)
            val pid = stringField(obj, "gnafPid")
            val house = stringField(obj, "houseNumber")
            val street = stringField(obj, "street")
            val unit = stringField(obj, "unit")
            val lat = doubleField(obj, "latitude")
            val lon = doubleField(obj, "longitude")
            val locality = stringField(obj, "locality")
            val postcode = stringField(obj, "postcode")
            val state = stringField(obj, "state")
            if (pid != null && house != null && street != null && locality != null &&
                lat != null && lon != null
            ) {
                results += GnafAddress(
                    gnafPid = pid,
                    houseNumber = house,
                    street = street,
                    unit = unit,
                    latitude = lat,
                    longitude = lon,
                    locality = locality,
                    postcode = postcode,
                    state = state,
                )
            }
            index = objEnd + 1
            // Stop after addresses array ends roughly when depth returns — rely on
            // missing required fields for trailing meta objects (none expected).
        }
        return results
    }

    private fun stringField(obj: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = pattern.find(obj) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .takeIf { it != "null" }
    }

    private fun doubleField(obj: String, key: String): Double? {
        val asString = stringField(obj, key)
        if (asString != null) return asString.toDoubleOrNull()
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return pattern.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
