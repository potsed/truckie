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
import com.truckie1.routing.StubGraphHopperPathRouter
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
    fun flagOffReturnsEmptyPoints() {
        val flags = LocalFeatureFlags()
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = StubGraphHopperPathRouter(),
            composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter()),
        )
        assertTrue(result is MapRoutePolylineGeometry.Result.FlagOff)
        assertTrue(MapRoutePolylineGeometry.pointsOrEmpty(result).isEmpty())
    }

    @Test
    fun prefersOptimisedTourGeometryWhenAvailable() {
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
        val trackingRouter = TrackingPathRouter(StubGraphHopperPathRouter())
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
    fun composesPathRouterLegsInCurrentOrderWhenNoOptimisedPath() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true))
        val router = StubGraphHopperPathRouter()
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
    fun prefersNavigateActivePathWhenPresent() {
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
            pathRouter = StubGraphHopperPathRouter(),
            composer = MultiStopTourComposer(pathRouter = StubGraphHopperPathRouter()),
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
    fun hardBlocksUnderPassageUsingComposerAndPackBridges() {
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
            ),
        )
        // Path that crosses the under-low bridge corridor.
        val crossing = object : PathRouter {
            override fun path(
                start: com.truckie1.trips.RouteEndpoint,
                end: com.truckie1.trips.RouteEndpoint,
                preferences: com.truckie1.routing.TruckRoutingPreferences,
                customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
            ): PathResult = PathResult.Ok(
                RoutePath(
                    id = "cross",
                    points = listOf(
                        LatLngPoint(start.latitude, start.longitude),
                        LatLngPoint(underLow.latitude, underLow.longitude),
                        LatLngPoint(end.latitude, end.longitude),
                    ),
                    source = RoutePath.SOURCE_STUB,
                    preferences = preferences,
                    customModelSnippet = customModel?.toRequestSnippet(),
                ),
            )
        }
        val composer = MultiStopTourComposer(
            pathRouter = crossing,
            bridges = listOf(underLow),
            bridgePassageEnabled = true,
        )
        val result = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(stops = listOf(stopA)),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = crossing,
            composer = composer,
        )
        assertTrue(result is MapRoutePolylineGeometry.Result.Blocked)
        val reason = (result as MapRoutePolylineGeometry.Result.Blocked).reason.lowercase()
        assertTrue(reason.contains("under") || reason.contains("bridge"))
        assertTrue(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = listOf(
                    LatLngPoint(stopA.latitude!!, stopA.longitude!!),
                    LatLngPoint(underLow.latitude, underLow.longitude),
                    LatLngPoint(end.latitude, end.longitude),
                ),
                bridges = listOf(underLow),
                vehicleHeightM = vehicle.heightM,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun customModelEncodesAgreedConstraintsAndTollWhenFlagged() {
        val flagsOn = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_ROUTE_POLYLINE to true,
                FlagKeys.TOLLS to true,
            ),
        )
        val router = StubGraphHopperPathRouter()
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
    fun composeOrderedReusesMultiStopTourComposerWithoutPermutation() {
        val router = StubGraphHopperPathRouter()
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

        override fun path(
            start: com.truckie1.trips.RouteEndpoint,
            end: com.truckie1.trips.RouteEndpoint,
            preferences: com.truckie1.routing.TruckRoutingPreferences,
            customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
        ): PathResult {
            calls += 1
            return delegate.path(start, end, preferences, customModel)
        }
    }
}
