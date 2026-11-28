package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

/**
 * Gherkin: verified_address_autocomplete.feature — Nominatim AU provider + JSON parse.
 */
class NominatimAddressAutocompleteTest {

    @Test
    fun blankQueryReturnsEmptyWithoutCallingClient() = runBlocking {
        var called = false
        val client = AddressHttpClient { _, _ ->
            called = true
            "[]"
        }
        val autocomplete = NominatimAddressAutocomplete(client = client)
        assertTrue(autocomplete.suggest("").isEmpty())
        assertTrue(autocomplete.suggest("  ").isEmpty())
        assertFalseCalled(called)
    }

    @Test
    fun parsesNominatimJsonIntoVerifiedSuggestions() = runBlocking {
        val json = """
            [
              {
                "place_id": 240109959,
                "lat": "-27.4704528",
                "lon": "153.0260339",
                "display_name": "1 Queen Street, Brisbane City, Queensland, 4000, Australia"
              },
              {
                "place_id": 1,
                "lat": "-33.8688",
                "lon": "151.2093",
                "display_name": "Sydney NSW, Australia"
              }
            ]
        """.trimIndent()
        val client = AddressHttpClient { _, _ -> json }
        val autocomplete = NominatimAddressAutocomplete(client = client)

        val suggestions = autocomplete.suggest("Queen Street Brisbane")
        assertEquals(2, suggestions.size)
        assertEquals(
            "1 Queen Street, Brisbane City, Queensland, 4000, Australia",
            suggestions[0].label,
        )
        assertEquals("240109959", suggestions[0].placeId)
        assertEquals(-27.4704528, suggestions[0].latitude!!, 1e-7)
        assertEquals(153.0260339, suggestions[0].longitude!!, 1e-7)
        assertTrue(suggestions[0].isVerified)
        assertEquals("Sydney NSW, Australia", suggestions[1].label)
    }

    @Test
    fun searchUrlBiasesToAustraliaAndIncludesUserAgent() = runBlocking {
        var capturedUrl: String? = null
        var capturedUserAgent: String? = null
        val client = AddressHttpClient { url, userAgent ->
            capturedUrl = url
            capturedUserAgent = userAgent
            "[]"
        }
        val autocomplete = NominatimAddressAutocomplete(
            client = client,
            countryCodes = "au",
            userAgent = NominatimAddressAutocomplete.DEFAULT_USER_AGENT,
        )

        autocomplete.suggest("Brisbane")

        val url = requireNotNull(capturedUrl)
        assertTrue(url.startsWith(NominatimAddressAutocomplete.DEFAULT_BASE_URL))
        assertTrue(url.contains("countrycodes=au"))
        assertTrue(url.contains("format=json"))
        assertTrue(url.contains("q=Brisbane") || url.contains("q=Brisbane".replace(" ", "+")))
        assertEquals(NominatimAddressAutocomplete.DEFAULT_USER_AGENT, capturedUserAgent)
    }

    @Test
    fun malformedOrEmptyBodyYieldsNoSuggestions() = runBlocking {
        val bad = NominatimAddressAutocomplete(client = AddressHttpClient { _, _ -> "not-json" })
        assertTrue(bad.suggest("x").isEmpty())
        val empty = NominatimAddressAutocomplete(client = AddressHttpClient { _, _ -> "" })
        assertTrue(empty.suggest("x").isEmpty())
    }

    /**
     * Gherkin: address_autocomplete_housenumber.feature —
     * number-keeping query prefers housenumber/building over street centroid.
     */
    @Test
    fun prefersHousenumberHitFromNumberKeepingQuery() = runBlocking {
        val streetJson = """
            [
              {
                "place_id": 27935006,
                "lat": "-27.2543388",
                "lon": "152.9958419",
                "class": "highway",
                "type": "residential",
                "display_name": "Elsie Street, Brisbane, Queensland, 4503, Australia"
              }
            ]
        """.trimIndent()
        val buildingJson = """
            [
              {
                "place_id": 27624684,
                "lat": "-27.2544437",
                "lon": "152.9963696",
                "class": "landuse",
                "type": "residential",
                "display_name": "Elsium Rise, 1-31, Kallangur, Queensland, 4503, Australia",
                "address": { "house_number": "1-31", "road": "Elsie Street" }
              }
            ]
        """.trimIndent()
        val client = AddressHttpClient { url, _ ->
            when {
                url.contains("1-31%2C") || url.contains("1-31,") ||
                    (url.contains("Kallangur") && !url.contains("Elsie")) -> buildingJson
                else -> streetJson
            }
        }
        val autocomplete = NominatimAddressAutocomplete(client = client)
        val suggestions = autocomplete.suggest("1-31 Elsie Street Kallangur")
        assertTrue(suggestions.isNotEmpty())
        assertEquals(AddressPrecision.BUILDING, suggestions[0].precision)
        assertEquals(-27.2544437, suggestions[0].latitude!!, 1e-7)
        assertEquals(152.9963696, suggestions[0].longitude!!, 1e-7)
        assertTrue(suggestions[0].label.contains("1-31"))
    }

    @Test
    fun searchUrlRequestsAddressDetails() = runBlocking {
        var capturedUrl: String? = null
        val client = AddressHttpClient { url, _ ->
            capturedUrl = url
            "[]"
        }
        NominatimAddressAutocomplete(client = client).suggest("Brisbane")
        assertTrue(requireNotNull(capturedUrl).contains("addressdetails=1"))
    }

    @Test
    fun parsesNestedAddressDetailsWithoutLosingTopLevelFields() {
        val json = """
            [
              {
                "place_id": 27935006,
                "lat": "-27.2543388",
                "lon": "152.9958419",
                "display_name": "Elsie Street, Brisbane, Queensland, 4503, Australia",
                "address": {
                  "road": "Elsie Street",
                  "city": "Brisbane",
                  "state": "Queensland",
                  "postcode": "4503",
                  "country": "Australia"
                }
              }
            ]
        """.trimIndent()
        val suggestions = NominatimSearchParser.parse(json)
        assertEquals(1, suggestions.size)
        assertEquals(
            "Elsie Street, Brisbane, Queensland, 4503, Australia",
            suggestions[0].label,
        )
        assertEquals(-27.2543388, suggestions[0].latitude!!, 1e-7)
    }

    private fun assertFalseCalled(called: Boolean) {
        assertTrue("HTTP client must not be called for blank queries", !called)
    }
}
