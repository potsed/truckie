package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/next_destination_display.feature — upcoming stop address (ATNA-83).
 */
class NextDestinationDisplayCatalogTest {

    private val start = RouteEndpoint("Depot", -27.45, 153.01)
    private val end = RouteEndpoint("18 Christensen Road, Yatala QLD 4207", -27.78, 153.23)

    @Test
    fun flagKeyDefaultsOff() {
        assertEquals("ff_next_destination_display", FlagKeys.NEXT_DESTINATION_DISPLAY)
        assertEquals(FlagKeys.NEXT_DESTINATION_DISPLAY, NextDestinationDisplayCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY))
        assertFalse(NextDestinationDisplayCatalog.isEnabled(flags))
        flags.setEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY, true)
        assertTrue(NextDestinationDisplayCatalog.isEnabled(flags))
    }

    @Test
    fun shouldShowRequiresFlagAndActiveNavigate() {
        val off = LocalFeatureFlags()
        assertFalse(
            NextDestinationDisplayCatalog.shouldShow(off, navigating = true),
        )
        val on = LocalFeatureFlags(mapOf(FlagKeys.NEXT_DESTINATION_DISPLAY to true))
        assertFalse(
            NextDestinationDisplayCatalog.shouldShow(on, navigating = false),
        )
        assertTrue(
            NextDestinationDisplayCatalog.shouldShow(on, navigating = true),
        )
    }

    @Test
    fun prefersUpcomingStopAddressOverEnd() {
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = "42 South Street, Lytton QLD 4178",
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
                RouteStop(
                    id = "s2",
                    addressLabel = "55 Schneider Road, Eagle Farm QLD 4009",
                    kind = StopKind.DROP_OFF_UNLOAD,
                    order = 1,
                ),
            ),
        )
        assertEquals(
            "42 South Street, Lytton QLD 4178",
            NextDestinationDisplayCatalog.resolveAddress(route, navigating = true),
        )
        assertEquals(
            "55 Schneider Road, Eagle Farm QLD 4009",
            NextDestinationDisplayCatalog.resolveAddress(
                route,
                navigating = true,
                upcomingStopIndex = 1,
            ),
        )
    }

    @Test
    fun fallsBackToEndWhenNoIntermediateStops() {
        val route = Route(
            id = "r2",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
        )
        assertEquals(
            end.addressLabel,
            NextDestinationDisplayCatalog.resolveAddress(route, navigating = true),
        )
    }

    @Test
    fun hiddenWhenNotNavigatingOrMissingRoute() {
        val route = Route(
            id = "r3",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
        )
        assertNull(
            NextDestinationDisplayCatalog.resolveAddress(route, navigating = false),
        )
        assertNull(
            NextDestinationDisplayCatalog.resolveAddress(null, navigating = true),
        )
    }

    @Test
    fun turnGuidanceDoesNotReplaceAddress() {
        assertTrue(NextDestinationDisplayCatalog.showsAddressAlongsideTurnGuidance)
        assertFalse(NextDestinationDisplayCatalog.replacesAddressWithTurnOnly)
        assertTrue(NextDestinationDisplayCatalog.usesComposeMaterial3Banner)
        assertFalse(NextDestinationDisplayCatalog.touchesLineManager)
        assertFalse(NextDestinationDisplayCatalog.touchesMapRoutePathStyle)
    }

    @Test
    fun phoneIsCompactAndAndroidAutoMustShowClearly() {
        assertTrue(NextDestinationDisplayCatalog.phoneSurfaceIsCompact)
        assertEquals(1, NextDestinationDisplayCatalog.phoneMaxAddressLines)
        assertTrue(NextDestinationDisplayCatalog.androidAutoMustShowClearly)
        assertEquals("ATNA-10", NextDestinationDisplayCatalog.androidAutoTrackedBy)
    }

    @Test
    fun displayAddressNullWhenFlagOffEvenIfNavigating() {
        val flags = LocalFeatureFlags()
        val route = Route(
            id = "r4",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
        )
        assertNull(
            NextDestinationDisplayCatalog.displayAddress(
                flags = flags,
                route = route,
                navigating = true,
            ),
        )
        flags.setEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY, true)
        assertEquals(
            end.addressLabel,
            NextDestinationDisplayCatalog.displayAddress(
                flags = flags,
                route = route,
                navigating = true,
            ),
        )
    }
}
