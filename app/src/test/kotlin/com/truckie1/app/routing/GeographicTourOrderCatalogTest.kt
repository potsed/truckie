package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/geographic_tour_order.feature — flag + intermediate strip.
 */
class GeographicTourOrderCatalogTest {

    @Test
    fun flagKeyAndDefaultOffUntilSeeded() {
        assertEquals("ff_geographic_tour_order", FlagKeys.GEOGRAPHIC_TOUR_ORDER)
        assertEquals(FlagKeys.GEOGRAPHIC_TOUR_ORDER, GeographicTourOrderCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(GeographicTourOrderCatalog.isEnabled(flags))
        GeographicTourOrderCatalog.setEnabled(flags, true)
        assertTrue(GeographicTourOrderCatalog.isEnabled(flags))
    }

    @Test
    fun intermediateStopsDropSyntheticStartAndEnd() {
        val ordered = listOf(
            TripStop(id = "start", addressLabel = "Start", latitude = -27.4, longitude = 153.0),
            TripStop(id = "a", addressLabel = "A", latitude = -27.41, longitude = 153.01),
            TripStop(id = "b", addressLabel = "B", latitude = -27.42, longitude = 153.02),
            TripStop(id = "end", addressLabel = "End", latitude = -27.4, longitude = 153.0),
        )
        assertEquals(
            listOf("a", "b"),
            GeographicTourOrderCatalog.intermediateStops(ordered).map { it.id },
        )
    }
}
