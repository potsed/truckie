package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.BridgePassageConstraint
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.PathResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.routing.RoutingBridgeConstraint
import com.truckie1.routing.RoutingBridgePassage
import com.truckie1.routing.StubPathRouter
import com.truckie1.routing.TollPreference
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

/**
 * Gherkin: features/map_route_polyline.feature — constrained PathRouter geometry.
 */
class MapRoutePolylineGeometryTest {

    private val vehicle = Vehicle(
        rego = "ABC123",
        nickname = "Rigid",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = 2019,
        heightM = 3.5,
        widthM = 2.5,
        lengthM = 12.5,
        massT = 10.0,
        tareT = 8.5,
        gvmT = 26.0,
        gcmT = 42.5,
        axleCount = 3,
        networkClass = "GML",
    )

    private val start = RouteEndpoint("Depot", -27.42, 153.10)
    private val end = RouteEndpoint("Depot", -27.42, 153.10)
    private val stopA = RouteStop(
        id = "a",
        addressLabel = "Near",
        latitude = -27.43,
        longitude = 153.11,
        kind = StopKind.PICKUP_LOAD,
        order = 0,
    )
    private val stopB = RouteStop(
        id = "b",
        addressLabel = "Far",
        latitude = -27.70,
        longitude = 153.25,
        kind = StopKind.DROP_OFF_UNLOAD,
        order = 1,
    )

    private fun route(stops: List<RouteStop> = listOf(stopA, stopB)): Route =
        Route(
            id = "r1",
            name = "Airport run",
            dateEpochDay = 20_000L,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
            stops = stops,
        )

    @Test
    fun flagOffReturnsEmptyPoints() = runBlocking {
        val flags = LocalFeatureFlags()
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = StubPathRouter(),
            composer = MultiStopTourComposer(pathRouter = StubPathRouter()),
        )
        assertTrue(result is MapRoutePolylineGeometry.Result.FlagOff)
        assertTrue(MapRoutePolylineGeometry.pointsOrEmpty(result).isEmpty())
    }

    @Test
    fun prefersOptimisedTourGeometryWhenAvailable() = runBlocking {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true))
        val optimised = RoutePath(
            id = "optimised-tour",
            points = listOf(
                LatLngPoint(-27.42, 153.10),
                LatLngPoint(-27.45, 153.12),
                LatLngPoint(-27.70, 153.25),
                LatLngPoint(-27.42, 153.10),
            ),
            source = RoutePath.SOURCE_STUB,
        )
        val trackingRouter = TrackingPathRouter(StubPathRouter())
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = trackingRouter,
            composer = MultiStopTourComposer(pathRouter = trackingRouter),
            optimisedPath = optimised,
        )
        assertTrue(result is MapRoutePolylineGeometry.Result.Ready)
        val ready = result as MapRoutePolylineGeometry.Result.Ready
        assertEquals("optimised-tour", ready.path.id)
        assertEquals(optimised.points, ready.path.points)
        assertEquals(0, trackingRouter.calls)
    }

    @Test
    fun composesPathRouterLegsInCurrentOrderWhenNoOptimisedPath() = runBlocking {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true))
        val router = StubPathRouter()
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = MultiStopTourComposer(pathRouter = router),
            optimisedPath = null,
        )
        assertTrue(result is MapRoutePolylineGeometry.Result.Ready)
        val ready = result as MapRoutePolylineGeometry.Result.Ready
        assertTrue(ready.path.points.size >= 2)
        assertEquals(RoutePath.SOURCE_STUB, ready.path.source)
        // Must not be a two-point DIY stop-to-stop line.
        assertTrue(ready.path.points.size > 2)
        assertFalse(MapRoutePolylineCatalog.allowsDiyStraightLineBetweenStops)
    }

    @Test
    fun prefersNavigateActivePathWhenPresent() = runBlocking {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true))
        val navigate = RoutePath(
            id = "navigate-path",
            points = listOf(
                LatLngPoint(-27.42, 153.10),
                LatLngPoint(-27.50, 153.15),
                LatLngPoint(-27.42, 153.10),
            ),
            source = RoutePath.SOURCE_STUB,
        )
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = StubPathRouter(),
            composer = MultiStopTourComposer(pathRouter = StubPathRouter()),
            optimisedPath = RoutePath(
                id = "optimised",
                points = listOf(
                    LatLngPoint(-27.1, 153.0),
                    LatLngPoint(-27.2, 153.1),
                ),
            ),
            navigatePath = navigate,
        )
        val ready = result as MapRoutePolylineGeometry.Result.Ready
        assertEquals("navigate-path", ready.path.id)
    }

    @Test
    fun tourStopsIncludeStartStopsAndEndWithoutDiyGeometry() {
        val stops = MapRoutePolylineGeometry.tourStopsFrom(route())
        assertEquals(listOf("start", "a", "b", "end"), stops.map { it.id })
        assertTrue(stops.all { it.latitude != null && it.longitude != null })
    }

    @Test
    fun detoursAroundUnderPassageViaPathRouterNotBlankPolyline() = runBlocking {
        val underLow = RoutingBridgeConstraint(
            id = "under-low",
            latitude = -27.55,
            longitude = 153.18,
            clearanceHeightM = 3.0,
            passage = RoutingBridgePassage.UNDER,
        )
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_ROUTE_POLYLINE to true,
                FlagKeys.BRIDGE_PASSAGE to true,
                FlagKeys.PATH_BLOCK_DETOUR to true,
            ),
        )
        // Stub PathRouter simulates GH blocked-edge avoidance when avoid list set.
        val router = StubPathRouter(intermediatePoints = 8)
        val composer = MultiStopTourComposer(
            pathRouter = router,
            bridges = listOf(underLow),
            bridgePassageEnabled = true,
            pathBlockDetourEnabled = true,
        )
        // Chord from stopA → end crosses underLow mid-path on plain interpolate.
        val midRoute = route(
            stops = listOf(
                RouteStop(
                    id = "a",
                    addressLabel = "A",
                    latitude = -27.54,
                    longitude = 153.17,
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                ),
            ),
        ).copy(
            start = RouteEndpoint("Start", -27.54, 153.17),
            end = RouteEndpoint("End", -27.56, 153.19),
        )
        assertTrue(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = listOf(
                    LatLngPoint(-27.54, 153.17),
                    LatLngPoint(underLow.latitude, underLow.longitude),
                    LatLngPoint(-27.56, 153.19),
                ),
                bridges = listOf(underLow),
                vehicleHeightM = vehicle.heightM,
                bufferM = 0.15,
            ),
        )
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = midRoute,
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = composer,
        )
        assertTrue(
            "PathRouter detour must keep a drawable line; got $result",
            result is MapRoutePolylineGeometry.Result.Ready,
        )
        val points = MapRoutePolylineGeometry.pointsOrEmpty(result)
        assertTrue(points.size >= 2)
        assertFalse(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(underLow),
                vehicleHeightM = vehicle.heightM,
                bufferM = 0.15,
                allowedNearPoints = listOf(
                    LatLngPoint(-27.54, 153.17),
                    LatLngPoint(-27.56, 153.19),
                ),
            ),
        )
    }

    @Test
    fun customModelEncodesAgreedConstraintsAndTollWhenFlagged() = runBlocking {
        val flagsOn = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_ROUTE_POLYLINE to true,
                FlagKeys.TOLLS to true,
            ),
        )
        val router = StubPathRouter()
        val result = MapRoutePolylineGeometry.resolve(
            flags = flagsOn,
            mapHomeEnabled = true,
            route = route(stops = listOf(stopA)),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = MultiStopTourComposer(
                pathRouter = router,
                tollPreference = TollPreference.AVOID_ALL,
            ),
        )
        val ready = result as MapRoutePolylineGeometry.Result.Ready
        val snippet = ready.path.customModelSnippet
        assertNotNull(snippet)
        assertTrue(snippet!!.contains("block_under_low_clearance"))
        assertTrue(snippet.contains("over_deck_ok"))
        assertTrue(snippet.contains("avoid_weight_restricted"))
        assertTrue(snippet.contains("residential_soft_preference_only"))
        assertTrue(snippet.contains("AVOID_ALL"))
    }

    @Test
    fun composeOrderedReusesMultiStopTourComposerWithoutPermutation() = runBlocking {
        val router = StubPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val stops = listOf(
            TripStop("depot", "Depot", -27.42, 153.10),
            TripStop("far", "Far", -27.70, 153.25),
            TripStop("near", "Near", -27.43, 153.11),
        )
        val composed = composer.composeOrdered(
            stops = stops,
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(composed is OptimizeResult.Ok)
        val ok = composed as OptimizeResult.Ok
        // Current order preserved — far before near (optimize would swap).
        assertEquals(listOf("depot", "far", "near"), ok.orderedStops.map { it.id })
        assertNotNull(ok.path)
        assertTrue(ok.path!!.points.size >= 2)
    }

    private class TrackingPathRouter(
        private val delegate: PathRouter,
    ) : PathRouter {
        var calls: Int = 0
            private set

        override suspend fun path(
            start: com.truckie1.trips.RouteEndpoint,
            end: com.truckie1.trips.RouteEndpoint,
            preferences: com.truckie1.routing.TruckRoutingPreferences,
            customModel: com.truckie1.routing.TruckRoutingModel?,
            avoidUnderBridges: List<RoutingBridgeConstraint>,
        ): PathResult {
            calls += 1
            return delegate.path(start, end, preferences, customModel, avoidUnderBridges)
        }
    }
}
