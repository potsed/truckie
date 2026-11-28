package com.truckie1.app.trips

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default [AddressAutocomplete] using OpenStreetMap Nominatim search.
 * Biased to Australia via `countrycodes` (Nominatim usage policy: identify User-Agent).
 *
 * Prefers housenumber / building hits over highway street centroids (ATNA-108).
 * When free-text is street-only, retries structured and number-keeping queries.
 *
 * [suggest] is **suspend** and hops to [ioDispatcher] before blocking HTTP
 * (ATNA-88) so Compose never owns the network call from `remember`.
 */
class NominatimAddressAutocomplete(
    private val client: AddressHttpClient = DefaultAddressHttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val countryCodes: String = DEFAULT_COUNTRY_CODES,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val limit: Int = DEFAULT_LIMIT,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AddressAutocomplete {

    override suspend fun suggest(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return withContext(ioDispatcher) {
            try {
                val premise = AddressPremiseParser.parse(trimmed)
                var fallback: List<AddressSuggestion> = emptyList()
                var precise: List<AddressSuggestion> = emptyList()
                for (candidate in NominatimQueryPlanner.candidateQueries(trimmed, premise)) {
                    val hits = fetchSuggestions(candidate)
                    if (hits.isEmpty()) continue
                    val ranked = NominatimHitRanker.preferPrecise(hits, premise)
                    if (fallback.isEmpty()) fallback = ranked
                    if (!premise.hasPremise || ranked.any { it.isPremisePrecise }) {
                        precise = ranked
                        break
                    }
                }
                // Structured street+city when free-text variants stayed street-only.
                val locality = premise.locality
                if (precise.isEmpty() &&
                    !premise.streetName.isNullOrBlank() &&
                    !locality.isNullOrBlank()
                ) {
                    val streetLine = listOfNotNull(premise.houseNumber, premise.streetName)
                        .joinToString(" ")
                    val structured = fetchStructured(streetLine, locality)
                    if (structured.isNotEmpty()) {
                        val ranked = NominatimHitRanker.preferPrecise(structured, premise)
                        if (ranked.any { it.isPremisePrecise }) {
                            precise = ranked
                        } else if (fallback.isEmpty()) {
                            fallback = ranked
                        }
                    }
                }
                val chosen = precise.ifEmpty { fallback }
                AddressSuggestionLabelEnricher.enrich(
                    premise = premise,
                    suggestions = chosen,
                )
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun fetchSuggestions(query: String): List<AddressSuggestion> {
        val body = client.get(buildSearchUrl(query), userAgent)
        return NominatimSearchParser.parse(body)
    }

    private fun fetchStructured(street: String, city: String): List<AddressSuggestion> {
        val body = client.get(buildStructuredSearchUrl(street, city), userAgent)
        return NominatimSearchParser.parse(body)
    }

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return "$baseUrl?q=$encoded&format=json&addressdetails=1" +
            "&limit=$limit&countrycodes=$countryCodes"
    }

    fun buildStructuredSearchUrl(
        street: String,
        city: String,
        country: String = "Australia",
    ): String {
        val s = URLEncoder.encode(street.trim(), StandardCharsets.UTF_8.name())
        val c = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8.name())
        val co = URLEncoder.encode(country.trim(), StandardCharsets.UTF_8.name())
        return "$baseUrl?street=$s&city=$c&country=$co&format=json&addressdetails=1" +
            "&limit=$limit"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/search"
        const val DEFAULT_COUNTRY_CODES = "au"
        const val DEFAULT_LIMIT = 5
        const val DEFAULT_USER_AGENT =
            "TruckieAndroid/0.8 (https://github.com/potsed/truckie; address-autocomplete)"
        const val SOURCE_ID = "nominatim"
    }
}

/**
 * Builds Nominatim query variants that keep typed house numbers when possible.
 */
object NominatimQueryPlanner {
    fun candidateQueries(
        query: String,
        premise: AddressPremiseParser.Premise,
    ): List<String> {
        val out = linkedSetOf<String>()
        out += query.trim()
        val house = premise.houseNumber
        val street = premise.streetName
        val locality = premise.locality
        if (!house.isNullOrBlank() && !street.isNullOrBlank()) {
            out += "$house $street".trim()
            if (!locality.isNullOrBlank()) {
                out += "$house $street $locality".trim()
                // Number-keeping form that finds OSM complexes (e.g. Elsium Rise).
                out += "$house, $locality"
                out += "$house $locality"
            }
        } else if (!house.isNullOrBlank() && !locality.isNullOrBlank()) {
            out += "$house, $locality"
        }
        if (premise.hasPremise && premise.remainder.isNotBlank()) {
            out += premise.remainder
        }
        return out.toList()
    }
}

/**
 * Ranks Nominatim hits so housenumber / building results beat street centroids.
 */
object NominatimHitRanker {
    fun preferPrecise(
        suggestions: List<AddressSuggestion>,
        premise: AddressPremiseParser.Premise,
    ): List<AddressSuggestion> {
        if (suggestions.isEmpty()) return suggestions
        val scored = suggestions.sortedByDescending { score(it, premise) }
        if (!premise.hasPremise) return scored
        val precise = scored.filter {
            it.precision == AddressPrecision.HOUSE ||
                it.precision == AddressPrecision.BUILDING ||
                it.precision == AddressPrecision.UNIT
        }
        return if (precise.isNotEmpty()) precise + scored.filter { it !in precise } else scored
    }

    private fun score(
        suggestion: AddressSuggestion,
        premise: AddressPremiseParser.Premise,
    ): Int {
        var s = when (suggestion.precision) {
            AddressPrecision.UNIT -> 40
            AddressPrecision.HOUSE -> 30
            AddressPrecision.BUILDING -> 25
            AddressPrecision.STREET -> 5
            AddressPrecision.UNKNOWN -> 10
        }
        val house = premise.houseNumber
        if (!house.isNullOrBlank() && suggestion.label.contains(house, ignoreCase = true)) {
            s += 20
        }
        return s
    }
}

/**
 * Parses Nominatim search JSON into verified [AddressSuggestion]s.
 * Supports nested `address` objects from `addressdetails=1` via brace balancing.
 */
object NominatimSearchParser {
    fun parse(body: String): List<AddressSuggestion> {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("[")) return emptyList()
        val results = mutableListOf<AddressSuggestion>()
        var index = 0
        while (index < trimmed.length) {
            val objStart = trimmed.indexOf('{', index)
            if (objStart < 0) break
            val objEnd = endOfJsonObject(trimmed, objStart)
            if (objEnd < 0) break
            val obj = trimmed.substring(objStart, objEnd + 1)
            val label = stringField(obj, "display_name")
            val lat = doubleField(obj, "lat")
            val lon = doubleField(obj, "lon")
            val placeId = stringField(obj, "place_id") ?: longFieldAsString(obj, "place_id")
            val houseNumber = stringField(obj, "house_number")
            val clazz = stringField(obj, "class")
            val type = stringField(obj, "type")
            if (!label.isNullOrBlank() && lat != null && lon != null) {
                results += AddressSuggestion(
                    label = label,
                    placeId = placeId,
                    latitude = lat,
                    longitude = lon,
                    precision = inferPrecision(houseNumber, clazz, type),
                    source = NominatimAddressAutocomplete.SOURCE_ID,
                )
            }
            index = objEnd + 1
        }
        return results
    }

    internal fun inferPrecision(
        houseNumber: String?,
        clazz: String?,
        type: String?,
    ): AddressPrecision {
        val hasHouse = !houseNumber.isNullOrBlank()
        val c = clazz?.lowercase().orEmpty()
        val t = type?.lowercase().orEmpty()
        return when {
            c == "highway" && !hasHouse -> AddressPrecision.STREET
            hasHouse && (c == "building" || c == "place" || t == "house" || t == "apartments") ->
                AddressPrecision.HOUSE
            hasHouse && (c == "landuse" || c == "residential") -> AddressPrecision.BUILDING
            hasHouse -> AddressPrecision.HOUSE
            c == "building" -> AddressPrecision.BUILDING
            c == "highway" -> AddressPrecision.STREET
            else -> AddressPrecision.UNKNOWN
        }
    }

    /** Index of the matching `}` for the `{` at [start], or -1 if unbalanced. */
    internal fun endOfJsonObject(source: String, start: Int): Int {
        if (start < 0 || start >= source.length || source[start] != '{') return -1
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until source.length) {
            val c = source[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun stringField(obj: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = pattern.find(obj) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun longFieldAsString(obj: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return pattern.find(obj)?.groupValues?.get(1)
    }

    private fun doubleField(obj: String, key: String): Double? {
        val asString = stringField(obj, key)
        if (asString != null) return asString.toDoubleOrNull()
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return pattern.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

/** Production HTTP client using [HttpURLConnection]. */
object DefaultAddressHttpClient : AddressHttpClient {
    override fun get(url: String, userAgent: String): String {
        val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: return ""
            }
            stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
