package com.truckie1.app.trips

import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

/**
 * Gherkin: verified_address_autocomplete.feature — persist formatted address + lat/lng.
 */
class VerifiedRouteStopBuilderTest {

    private val brisbane = AddressSuggestion(
        label = "1 Queen Street, Brisbane City, Queensland, 4000, Australia",
        placeId = "240109959",
        latitude = -27.4704528,
        longitude = 153.0260339,
    )

    @Test
    fun verifiedSuggestionPersistsLabelAndCoordinates() {
        val result = VerifiedRouteStopBuilder.build(
            id = "s1",
            kind = StopKind.PICKUP_LOAD,
            suggestion = brisbane,
            freeTextLabel = "ignored when suggestion present",
            requireVerified = true,
        )
        assertTrue(result is VerifiedRouteStopBuilder.Result.Ok)
        val stop = (result as VerifiedRouteStopBuilder.Result.Ok).stop
        assertEquals(brisbane.label, stop.addressLabel)
        assertEquals(brisbane.latitude, stop.latitude)
        assertEquals(brisbane.longitude, stop.longitude)
        assertEquals(StopKind.PICKUP_LOAD, stop.kind)
        assertEquals("s1", stop.id)
    }

    @Test
    fun requireVerifiedRejectsFreeTextOnly() {
        val result = VerifiedRouteStopBuilder.build(
            id = "s2",
            kind = StopKind.DROP_OFF_UNLOAD,
            suggestion = null,
            freeTextLabel = "some unverified typed address",
            requireVerified = true,
        )
        assertTrue(result is VerifiedRouteStopBuilder.Result.Err)
        assertEquals(
            VerifiedRouteStopBuilder.ERR_VERIFIED_REQUIRED,
            (result as VerifiedRouteStopBuilder.Result.Err).message,
        )
    }

    @Test
    fun requireVerifiedRejectsSuggestionMissingCoordinates() {
        val incomplete = AddressSuggestion(
            label = "Somewhere",
            placeId = null,
            latitude = null,
            longitude = null,
        )
        val result = VerifiedRouteStopBuilder.build(
            id = "s3",
            kind = StopKind.PICKUP_LOAD,
            suggestion = incomplete,
            freeTextLabel = null,
            requireVerified = true,
        )
        assertTrue(result is VerifiedRouteStopBuilder.Result.Err)
    }

    @Test
    fun whenNotRequiredFreeTextStillBuildsStopWithoutCoords() {
        val result = VerifiedRouteStopBuilder.build(
            id = "s4",
            kind = StopKind.DROP_OFF_UNLOAD,
            suggestion = null,
            freeTextLabel = "Depot yard",
            requireVerified = false,
        )
        assertTrue(result is VerifiedRouteStopBuilder.Result.Ok)
        val stop = (result as VerifiedRouteStopBuilder.Result.Ok).stop
        assertEquals("Depot yard", stop.addressLabel)
        assertNull(stop.latitude)
        assertNull(stop.longitude)
    }

    @Test
    fun fakeProviderSuggestionBuildsVerifiedStop() = runBlocking {
        val fake = AddressAutocomplete { query ->
            if (query.contains("Brisbane", ignoreCase = true)) listOf(brisbane) else emptyList()
        }
        val suggestion = fake.suggest("Brisbane").single()
        val result = VerifiedRouteStopBuilder.build(
            id = "s5",
            kind = StopKind.PICKUP_LOAD,
            suggestion = suggestion,
            freeTextLabel = null,
            requireVerified = true,
        )
        assertTrue(result is VerifiedRouteStopBuilder.Result.Ok)
        val stop = (result as VerifiedRouteStopBuilder.Result.Ok).stop
        assertTrue(stop.latitude != null && stop.longitude != null)
    }
}
