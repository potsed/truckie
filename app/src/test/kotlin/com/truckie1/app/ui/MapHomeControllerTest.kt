package com.truckie1.app.ui

import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.StubPathRouter
import com.truckie1.trips.ActiveTourSession
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_home_orchestration.feature — MapHomeController owns domain.
 */
class MapHomeControllerTest {

    private fun controller(
        flags: LocalFeatureFlags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.ROUTE_START_END to true,
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.BRIDGE_MARKERS to true,
            ),
        ),
        bridges: List<BridgePoint> = emptyList(),
        heightM: Double? = null,
        bufferM: Double = 0.3,
    ): MapHomeController {
        val tour = ActiveTourSession()
        return MapHomeController(
            routeSession = tour,
            navigateSession = RouteNavigateSession(
                composerProvider = { MultiStopTourComposer(StubPathRouter()) },
            ),
            polylineSession = MapRoutePolylineSession(),
            flags = flags,
            bridgeLoader = { bridges },
            activeVehicleHeightM = { heightM },
            clearanceBufferM = { bufferM },
            stopIdFactory = { "stop-1" },
        )
    }

    private fun verified(label: String, lat: Double, lng: Double) =
        AddressSuggestion(label = label, latitude = lat, longitude = lng)

    @Test
    fun createRouteRequiresVerifiedEndpointsWhenStartEndOn() {
        val c = controller()
        assertNull(c.route)

        val rejected = c.createRoute(
            CreateRouteInput(
                name = "Run",
                dateChoice = RouteDateChoice.TODAY,
                customDate = null,
                start = null,
                end = null,
            ),
        )
        assertTrue(!rejected)
        assertNull(c.route)

        val ok = c.createRoute(
            CreateRouteInput(
                name = "Run",
                dateChoice = RouteDateChoice.TODAY,
                customDate = null,
                start = verified("Depot", -27.4, 153.0),
                end = verified("Yard", -27.5, 153.1),
            ),
        )
        assertTrue(ok)
        assertNotNull(c.route)
        assertEquals(RouteSheetValue.Half, c.sheetValue)
        assertTrue(c.navigatePathPoints.isEmpty())
        assertEquals(1, c.polylineRevision)
    }

    @Test
    fun addStopUpdatesActiveRouteAndBumpsPolyline() {
        val c = controller()
        assertTrue(
            c.createRoute(
                CreateRouteInput(
                    name = "Run",
                    dateChoice = RouteDateChoice.TODAY,
                    customDate = null,
                    start = verified("Depot", -27.4, 153.0),
                    end = verified("Yard", -27.5, 153.1),
                ),
            ),
        )
        val before = c.polylineRevision
        assertTrue(
            c.addStop(
                addressLabel = "Customer",
                kind = StopKind.DROP_OFF_UNLOAD,
                suggestion = verified("Customer", -27.45, 153.05),
            ),
        )
        assertEquals(listOf("stop-1"), c.route!!.stops.map { it.id })
        assertEquals(before + 1, c.polylineRevision)
    }

    @Test
    fun demoRebindClearsNavigateAndOpensSheetWhenStopsPresent() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.ROUTE_START_END to true,
                FlagKeys.ADDRESS_AUTOCOMPLETE to true,
                FlagKeys.DEMO_MODE to true,
            ),
        )
        val c = controller(flags = flags)
        assertTrue(
            c.createRoute(
                CreateRouteInput(
                    name = "Demo",
                    dateChoice = RouteDateChoice.TODAY,
                    customDate = null,
                    start = verified("Depot", -27.4, 153.0),
                    end = verified("Yard", -27.5, 153.1),
                ),
            ),
        )
        assertTrue(
            c.addStop(
                addressLabel = "Stop",
                kind = StopKind.PICKUP_LOAD,
                suggestion = verified("Stop", -27.42, 153.02),
            ),
        )
        c.setSheetValue(RouteSheetValue.Peek)
        c.onDemoRevision()
        assertEquals(RouteSheetValue.Half, c.sheetValue)
        assertTrue(c.navigatePathPoints.isEmpty())
    }

    @Test
    fun locationFixFeedsApproachPoint() {
        val c = controller()
        c.onLocationFix(LocationFix(latitude = -27.1, longitude = 153.2))
        assertEquals(LatLngPoint(-27.1, 153.2), c.currentLocation)
        c.onLocationFix(null)
        assertNull(c.currentLocation)
    }

    @Test
    fun visibleBridgesUseSinglePipeline() {
        val bridges = listOf(
            BridgePoint(
                id = "b1",
                name = "Sample",
                latitude = -27.4,
                longitude = 153.0,
                clearanceHeightM = 3.5,
            ),
        )
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.BRIDGE_MARKERS to true,
            ),
        )
        val c = controller(flags = flags, bridges = bridges)
        assertEquals(bridges, c.visibleBridges(showBridgeMarkers = true))
        assertTrue(c.visibleBridges(showBridgeMarkers = false).isEmpty())
    }

    @Test
    fun uiStateSnapshotIncludesSummaryAndPath() {
        val c = controller()
        assertTrue(
            c.createRoute(
                CreateRouteInput(
                    name = "Run",
                    dateChoice = RouteDateChoice.TODAY,
                    customDate = null,
                    start = verified("Depot", -27.4, 153.0),
                    end = verified("Yard", -27.5, 153.1),
                ),
            ),
        )
        val path = listOf(LatLngPoint(-27.4, 153.0), LatLngPoint(-27.5, 153.1))
        val state = c.uiState(displayPathPoints = path)
        assertEquals(c.route, state.route)
        assertEquals(c.sheetValue, state.sheetValue)
        assertEquals(path, state.navigatePathPoints)
        assertTrue(state.summaryStrip.isNotBlank())
    }
}
