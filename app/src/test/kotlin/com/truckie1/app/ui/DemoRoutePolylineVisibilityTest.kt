package com.truckie1.app.ui

import com.truckie1.app.routing.ActiveTourPath
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.RoutingBridgeConstraint
import com.truckie1.routing.RoutingBridgePassage
import com.truckie1.routing.StubGraphHopperPathRouter
import com.truckie1.trips.ActiveTourFacade
import com.truckie1.trips.ActiveTourSession
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Demo Airport run + Colina St: stop markers must not leave LineManager empty
 * (ATNA-101). Gherkin: features/map_route_polyline.feature,
 * features/demo_mode.feature, features/bridge_passage_pipeline.feature.
 *
 * Root cause: under-passage hard-block emptied LineManager. Fix: stop-proximity
 * allowance + PathBlockDetour via PathRouter (live GH preferred; stub in tests).
 */
class DemoRoutePolylineVisibilityTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 28)

    private val colina = RoutingBridgeConstraint(
        id = DemoDataset.NEAR_LOW_BRIDGE_ID,
        latitude = DemoDataset.NEAR_LOW_BRIDGE_LAT,
        longitude = DemoDataset.NEAR_LOW_BRIDGE_LNG,
        clearanceHeightM = DemoDataset.NEAR_LOW_BRIDGE_CLEARANCE_M,
        passage = RoutingBridgePassage.UNDER,
    )

    @Test
    fun demoAirportRunStillProvidesActiveRouteAfterRoomRealStoreSwap() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.DEMO_MODE to true,
                FlagKeys.ROOM_ROUTE_PERSIST to true,
                FlagKeys.MAP_ROUTE_POLYLINE to true,
            ),
        )
        // Real store can be empty (Room-backed in prod); demo stays in-memory seed.
        val dual = DualModeAppDataSource(
            real = RealAppDataSource(
                vehicles = InMemoryVehicleProfileRepository(),
                tour = ActiveTourSession(),
            ),
            demo = DemoAppDataSource.create(today = today),
            flags = flags,
        )
        val demoTour = dual.demoSource.routes as ActiveTourFacade
        assertTrue(RoomRoutePersistCatalog.isInMemorySession(demoTour))
        val route = dual.routes.activeRoute()
        assertNotNull(route)
        assertEquals(DemoDataset.ROUTE_NAME, route!!.name)
        assertEquals(DemoDataset.ROUTE_ID, route.id)
        assertEquals(6, route.stops.size)
        assertTrue(route.hasEndpoints)
        assertEquals(DemoDataset.VEHICLE_REGO, dual.vehicles.activeRego)
        assertNotNull(dual.vehicles.findByRego(DemoDataset.VEHICLE_REGO))
    }

    @Test
    fun demoAirportRunPolylineReadyWithColinaUnderBridgeAndApproach() = runBlocking {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_ROUTE_POLYLINE to true,
                FlagKeys.BRIDGE_PASSAGE to true,
                FlagKeys.ROUTE_PATH_FROM_LOCATION to true,
            ),
        )
        val route = DemoDataset.buildRoute(today = today)
        val vehicle = DemoDataset.vehicle
        val router = StubGraphHopperPathRouter()
        val composer = MultiStopTourComposer(
            pathRouter = router,
            bridges = listOf(colina),
            bridgePassageEnabled = true,
        )
        // Blue-dot near Kallangur (Daniel screenshot) — approach prepended.
        val current = LatLngPoint(-27.2505, 152.9955)
        val composed = ActiveTourPath.compose(
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
            composer = composer,
            currentLocation = current,
        )
        assertTrue(
            "ActiveTourPath must Ready for demo Airport run; got $composed",
            composed is ActiveTourPath.Result.Ready,
        )
        val path = (composed as ActiveTourPath.Result.Ready).path
        assertTrue(path.points.size >= 2)
        assertEquals(
            ActiveTourPath.CURRENT_LOCATION_ID,
            ActiveTourPath.orderedWaypoints(route, current).first().id,
        )

        val polyline = MapRoutePolylineGeometry.resolve(
            flags = flags,
            mapHomeEnabled = true,
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
            pathRouter = router,
            composer = composer,
            currentLocation = current,
        )
        assertTrue(
            "Map polyline must Ready so LineManager gets points; got $polyline",
            polyline is MapRoutePolylineGeometry.Result.Ready,
        )
        val points = MapRoutePolylineGeometry.pointsOrEmpty(polyline)
        assertTrue(points.size >= 2)
        assertTrue(MapRoutePolylineCatalog.usesLineManager)
        assertTrue(MapLineBind.sharesPathWithNavigatePolyline)
    }
}
