package com.truckie1.app.trips

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Default [AddressAutocomplete] using OpenStreetMap Nominatim search.
 * Biased to Australia via `countrycodes` (Nominatim usage policy: identify User-Agent).
 */
class NominatimAddressAutocomplete(
    private val client: AddressHttpClient = DefaultAddressHttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val countryCodes: String = DEFAULT_COUNTRY_CODES,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val limit: Int = DEFAULT_LIMIT,
) : AddressAutocomplete {

    override fun suggest(query: String): List<AddressSuggestion> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val url = buildSearchUrl(trimmed)
            val body = client.get(url, userAgent)
            NominatimSearchParser.parse(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return "$baseUrl?q=$encoded&format=json&addressdetails=0" +
            "&limit=$limit&countrycodes=$countryCodes"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/search"
        const val DEFAULT_COUNTRY_CODES = "au"
        const val DEFAULT_LIMIT = 5
        const val DEFAULT_USER_AGENT =
            "TruckieAndroid/0.8 (https://github.com/potsed/truckie; address-autocomplete)"
    }
}

/**
 * Parses Nominatim search JSON into verified [AddressSuggestion]s.
 * Avoids a JSON library dependency — Nominatim fields are simple scalars.
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
            val objEnd = trimmed.indexOf('}', objStart)
            if (objEnd < 0) break
            val obj = trimmed.substring(objStart, objEnd + 1)
            val label = stringField(obj, "display_name")
            val lat = doubleField(obj, "lat")
            val lon = doubleField(obj, "lon")
            val placeId = stringField(obj, "place_id") ?: longFieldAsString(obj, "place_id")
            if (!label.isNullOrBlank() && lat != null && lon != null) {
                results += AddressSuggestion(
                    label = label,
                    placeId = placeId,
                    latitude = lat,
                    longitude = lon,
                )
            }
            index = objEnd + 1
        }
        return results
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
