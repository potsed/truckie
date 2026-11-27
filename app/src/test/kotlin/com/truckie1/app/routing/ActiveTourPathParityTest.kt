package com.truckie1.app.routing

import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.MapRoutePolylineGeometry
import com.truckie1.app.ui.NavigateMultiStopCatalog
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.PathResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.routing.StubGraphHopperPathRouter
import com.truckie1.routing.TruckRoutingPreferences
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/navigate_multi_stop_path.feature — Navigate path ==
 * MultiStopTourComposer geometry (ATNA-87 / P1-2). This parity assertion
 * fails against the legacy start→end-only Navigate path.
 */
class ActiveTourPathParityTest {

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
    private val stopNear = RouteStop(
        id = "near",
        addressLabel = "Near",
        latitude = -27.43,
        longitude = 153.11,
        kind = StopKind.PICKUP_LOAD,
        order = 0,
    )
    private val stopFar = RouteStop(
        id = "far",
        addressLabel = "Far",
        latitude = -27.70,
        longitude = 153.25,
        kind = StopKind.DROP_OFF_UNLOAD,
        order = 1,
    )

    private fun multiStopRoute(): Route = Route(
        id = "r1",
        name = "Airport run",
        dateEpochDay = 20_000L,
        status = RouteStatus.ACTIVE,
        start = start,
        end = end,
        stops = listOf(stopNear, stopFar),
    )

    private fun flagsOn(): LocalFeatureFlags = LocalFeatureFlags(
        mapOf(
            FlagKeys.NAVIGATE to true,
            FlagKeys.NAVIGATE_MULTI_STOP to true,
        ),
    )

    @Test
    fun navigatePathMatchesComposerGeometryForSameRoute() {
        val router = StubGraphHopperPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val route = multiStopRoute()
        val session = RouteNavigateSession(composerProvider = { composer })

        val navigate = session.start(
            flags = flagsOn(),
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
        )
        assertTrue(navigate is RouteNavigateSession.Result.Ready)
        val navigatePath = (navigate as RouteNavigateSession.Result.Ready).path

        val tourStops = ActiveTourPath.tourStopsFrom(route)
        val composed = composer.composeOrdered(
            stops = tourStops,
            vehicle = vehicle,
            bufferM = 0.15,
        )
        assertTrue(composed is OptimizeResult.Ok)
        val composerPath = requireNotNull((composed as OptimizeResult.Ok).path)

        assertEquals(composerPath.points, navigatePath.points)
        assertEquals(composerPath.id, navigatePath.id)
    }

    @Test
    fun navigatePathIsNotStartToEndOnlyWhenStopsExist() {
        val router = StubGraphHopperPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val route = multiStopRoute()
        val session = RouteNavigateSession(composerProvider = { composer })

        val navigate = session.start(
            flags = flagsOn(),
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
        )
        val navigatePath = (navigate as RouteNavigateSession.Result.Ready).path

        val startEndOnly = when (
            val direct = router.path(start, end)
        ) {
            is PathResult.Ok -> direct.path
            is PathResult.Blocked -> error("stub should not block")
        }

        // Legacy Navigate called path(start, end) only — that stub skips stops.
        assertNotEquals(startEndOnly.points, navigatePath.points)
        assertTrue(navigatePath.points.size > startEndOnly.points.size)
        // Multi-stop path must visit stop latitudes (composer stitches legs).
        assertTrue(navigatePath.points.any { near(it, stopNear.latitude!!, stopNear.longitude!!) })
        assertTrue(navigatePath.points.any { near(it, stopFar.latitude!!, stopFar.longitude!!) })
    }

    @Test
    fun navigateAndPolylineShareActiveTourPathGeometry() {
        val router = StubGraphHopperPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val route = multiStopRoute()
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAVIGATE to true,
                FlagKeys.NAVIGATE_MULTI_STOP to true,
                FlagKeys.MAP_ROUTE_POLYLINE to true,
            ),
        )
        val session = RouteNavigateSession(composerProvider = { composer })
        val navigate = session.start(
            flags = flags,
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
        )
        val navigatePath = (navigate as RouteNavigateSession.Result.Ready).path

        val polyline = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = composer,
            navigatePath = null,
            optimisedPath = null,
        )
        assertTrue(polyline is MapRoutePolylineGeometry.Result.Ready)
        val polylinePath = (polyline as MapRoutePolylineGeometry.Result.Ready).path

        assertEquals(navigatePath.points, polylinePath.points)
        assertTrue(NavigateMultiStopCatalog.usesActiveTourPath)
        assertFalse(NavigateMultiStopCatalog.allowsStartToEndOnlyPath)
    }

    @Test
    fun navigatePrefersCachedOptimisedTourGeometry() {
        val router = RecordingPathRouter(StubGraphHopperPathRouter())
        val composer = MultiStopTourComposer(pathRouter = router)
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
        val session = RouteNavigateSession(composerProvider = { composer })
        val navigate = session.start(
            flags = flagsOn(),
            route = multiStopRoute(),
            vehicle = vehicle,
            bufferM = 0.15,
            optimisedPath = optimised,
        )
        val path = (navigate as RouteNavigateSession.Result.Ready).path
        assertEquals("optimised-tour", path.id)
        assertEquals(optimised.points, path.points)
        assertEquals(0, router.calls)
    }

    private fun near(point: LatLngPoint, lat: Double, lng: Double, eps: Double = 1e-6): Boolean =
        kotlin.math.abs(point.latitude - lat) < eps &&
            kotlin.math.abs(point.longitude - lng) < eps

    private class RecordingPathRouter(
        private val delegate: PathRouter,
    ) : PathRouter {
        var calls: Int = 0
            private set

        override fun path(
            start: RouteEndpoint,
            end: RouteEndpoint,
            preferences: TruckRoutingPreferences,
            customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
        ): PathResult {
            calls += 1
            return delegate.path(start, end, preferences, customModel)
        }
    }
}
