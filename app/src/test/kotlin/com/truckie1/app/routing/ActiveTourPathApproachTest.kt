package com.truckie1.app.routing

import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.MapRoutePolylineGeometry
import com.truckie1.app.ui.RoutePathFromLocationCatalog
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.StubPathRouter
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
import kotlinx.coroutines.runBlocking

/**
 * Gherkin: features/route_path_from_location.feature — current → start →
 * stops → end (ATNA-99). Fails when ActiveTourPath omits the approach leg.
 */
class ActiveTourPathApproachTest {

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
    private val current = LatLngPoint(-27.40, 153.08)

    private fun route(): Route = Route(
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
            FlagKeys.MAP_ROUTE_POLYLINE to true,
            FlagKeys.ROUTE_PATH_FROM_LOCATION to true,
        ),
    )

    @Test
    fun orderedWaypointsAreCurrentThenStartThenStopsThenEnd() {
        val waypoints = ActiveTourPath.orderedWaypoints(route(), current)
        assertEquals(5, waypoints.size) // current + start + 2 stops + end
        assertEquals(ActiveTourPath.CURRENT_LOCATION_ID, waypoints[0].id)
        assertEquals(current.latitude, waypoints[0].latitude!!, 0.0)
        assertEquals(current.longitude, waypoints[0].longitude!!, 0.0)
        assertEquals("start", waypoints[1].id)
        assertEquals("near", waypoints[2].id)
        assertEquals("far", waypoints[3].id)
        assertEquals("end", waypoints[4].id)
    }

    @Test
    fun orderedWaypointsWithoutCurrentStartAtRouteStart() {
        val waypoints = ActiveTourPath.orderedWaypoints(route(), currentLocation = null)
        assertEquals("start", waypoints.first().id)
        assertFalse(waypoints.any { it.id == ActiveTourPath.CURRENT_LOCATION_ID })
    }

    @Test
    fun composePathStartsAtCurrentThenVisitsStartStopsEnd() = runBlocking {
        val router = StubPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val result = ActiveTourPath.compose(
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            composer = composer,
            currentLocation = current,
        )
        assertTrue(result is ActiveTourPath.Result.Ready)
        val path = (result as ActiveTourPath.Result.Ready).path
        assertTrue(near(path.points.first(), current.latitude, current.longitude))
        assertTrue(path.points.any { near(it, start.latitude, start.longitude) })
        assertTrue(path.points.any { near(it, stopNear.latitude!!, stopNear.longitude!!) })
        assertTrue(path.points.any { near(it, stopFar.latitude!!, stopFar.longitude!!) })
        assertTrue(near(path.points.last(), end.latitude, end.longitude))
    }

    @Test
    fun composeWithoutCurrentDoesNotMatchPathThatIncludesApproach() = runBlocking {
        val router = StubPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val withApproach = ActiveTourPath.compose(
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            composer = composer,
            currentLocation = current,
        )
        val without = ActiveTourPath.compose(
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            composer = composer,
            currentLocation = null,
        )
        val a = (withApproach as ActiveTourPath.Result.Ready).path
        val b = (without as ActiveTourPath.Result.Ready).path
        assertNotEquals(a.points, b.points)
        assertTrue(a.points.size > b.points.size)
        assertTrue(near(b.points.first(), start.latitude, start.longitude))
    }

    @Test
    fun navigateAndPolylineShareApproachGeometry() = runBlocking {
        val router = StubPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val flags = flagsOn()
        val session = RouteNavigateSession(composerProvider = { composer })
        val navigate = session.start(
            flags = flags,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            currentLocation = current,
        )
        assertTrue(navigate is RouteNavigateSession.Result.Ready)
        val navigatePath = (navigate as RouteNavigateSession.Result.Ready).path

        val polyline = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = composer,
            currentLocation = current,
        )
        assertTrue(polyline is MapRoutePolylineGeometry.Result.Ready)
        val polylinePath = (polyline as MapRoutePolylineGeometry.Result.Ready).path

        assertEquals(navigatePath.points, polylinePath.points)
        assertTrue(near(navigatePath.points.first(), current.latitude, current.longitude))
        assertTrue(RoutePathFromLocationCatalog.DEFAULT_ENABLED)
        assertTrue(RoutePathFromLocationCatalog.usesActiveTourPathApproach)
    }

    @Test
    fun cachedOptimisedTourStillGetsApproachLegPrepended() = runBlocking {
        val router = StubPathRouter()
        val composer = MultiStopTourComposer(pathRouter = router)
        val tourOnly = when (
            val composed = composer.composeOrdered(
                stops = ActiveTourPath.tourStopsFrom(route()),
                vehicle = vehicle,
                bufferM = 0.15,
            )
        ) {
            is OptimizeResult.Ok -> requireNotNull(composed.path)
            is OptimizeResult.Blocked -> error(composed.reason)
        }
        // Exercises suspend withApproach → composeOrdered (cached Optimise + approach).
        // Would fail to compile if withApproach were still a sync caller of suspend composeOrdered.
        val result = ActiveTourPath.compose(
            route = route(),
            vehicle = vehicle,
            bufferM = 0.15,
            composer = composer,
            optimisedPath = tourOnly,
            currentLocation = current,
        )
        assertTrue(result is ActiveTourPath.Result.Ready)
        val path = (result as ActiveTourPath.Result.Ready).path
        assertTrue(path.id.startsWith("approach+"))
        assertTrue(near(path.points.first(), current.latitude, current.longitude))
        assertTrue(path.points.any { near(it, start.latitude, start.longitude) })
        assertTrue(path.points.any { near(it, stopNear.latitude!!, stopNear.longitude!!) })
        assertTrue(near(path.points.last(), end.latitude, end.longitude))
        assertTrue(path.points.size > tourOnly.points.size)
    }

    @Test
    fun composeAndWithApproachAreSuspendOnlyNoSyncResolvePath() {
        // One resolve path: suspend compose (+ private suspend withApproach).
        // Guards against leftover sync overloads / dual sync+async debt.
        fun isContinuation(type: Class<*>): Boolean =
            type.name == "kotlin.coroutines.Continuation"

        val composeMethods = ActiveTourPath::class.java.declaredMethods.filter {
            it.name == "compose" && !it.isSynthetic && !it.name.contains('$')
        }
        assertTrue(composeMethods.isNotEmpty())
        assertTrue(composeMethods.all { m -> m.parameterTypes.any(::isContinuation) })

        val withApproachMethods = ActiveTourPath::class.java.declaredMethods.filter {
            it.name == "withApproach" && !it.isSynthetic
        }
        assertEquals(1, withApproachMethods.size)
        assertTrue(withApproachMethods.single().parameterTypes.any(::isContinuation))
    }

    private fun near(point: LatLngPoint, lat: Double, lng: Double, eps: Double = 1e-6): Boolean =
        kotlin.math.abs(point.latitude - lat) < eps &&
            kotlin.math.abs(point.longitude - lng) < eps
}
