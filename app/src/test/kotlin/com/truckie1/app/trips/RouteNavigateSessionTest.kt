package com.truckie1.app.trips

import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.app.ui.NavigateMultiStopCatalog
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.PathResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.routing.StubGraphHopperPathRouter
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature + navigate_multi_stop_path.feature —
 * in-app GraphHopper Navigate via ActiveTourPath + optional handoff.
 */
class RouteNavigateSessionTest {

    private val start = RouteEndpoint("Depot", -27.45, 153.01)
    private val end = RouteEndpoint("Customer", -27.47, 153.03)

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

    private fun flagsNavigateOn(): LocalFeatureFlags = LocalFeatureFlags(
        mapOf(
            FlagKeys.NAVIGATE to true,
            FlagKeys.NAVIGATE_MULTI_STOP to true,
        ),
    )

    private fun session(
        router: PathRouter = StubGraphHopperPathRouter(),
    ): RouteNavigateSession = RouteNavigateSession(
        composerProvider = { MultiStopTourComposer(pathRouter = router) },
    )

    @Test
    fun flagKeyDefaultsOff() {
        assertEquals("ff_navigate", FlagKeys.NAVIGATE)
        assertEquals(FlagKeys.NAVIGATE, RouteNavigateCatalog.FLAG)
        val flags = LocalFeatureFlags()
        assertFalse(RouteNavigateCatalog.isEnabled(flags))
        flags.setEnabled(FlagKeys.NAVIGATE, true)
        assertTrue(RouteNavigateCatalog.isEnabled(flags))
    }

    @Test
    fun canNavigateRequiresFlagEndpointsAndVehicle() {
        val flagsOff = LocalFeatureFlags()
        val flagsOn = flagsNavigateOn()
        val ready = Route(
            id = "r1",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
        )
        assertFalse(RouteNavigateCatalog.canNavigate(flagsOff, ready, vehicle))
        assertFalse(RouteNavigateCatalog.canNavigate(flagsOn, null, vehicle))
        assertFalse(
            RouteNavigateCatalog.canNavigate(
                flagsOn,
                Route(id = "x", dateEpochDay = 1),
                vehicle,
            ),
        )
        assertFalse(RouteNavigateCatalog.canNavigate(flagsOn, ready, vehicle = null))
        assertTrue(RouteNavigateCatalog.canNavigate(flagsOn, ready, vehicle))
        assertTrue(RouteNavigateCatalog.canNavigate(flagsOn, ready.copy(end = start), vehicle))
    }

    @Test
    fun startAlwaysComputesInAppGraphHopperPath() {
        val flags = flagsNavigateOn()
        val navigateSession = session()
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            start = start,
            end = end,
        )
        when (
            val result = navigateSession.start(
                flags = flags,
                route = route,
                vehicle = vehicle,
                bufferM = 0.15,
                selection = NavigationAppSelection.InAppTruckie,
            )
        ) {
            is RouteNavigateSession.Result.Ready -> {
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
                assertTrue(result.path.points.size >= 2)
                assertNull(result.externalHandoff)
                assertEquals(result.path, navigateSession.activePath())
            }
            else -> error("expected Ready, got $result")
        }
    }

    @Test
    fun startRoundTripStillYieldsInAppPath() {
        val flags = flagsNavigateOn()
        val navigateSession = session()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = start)
        val result = navigateSession.start(
            flags = flags,
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
            selection = NavigationAppSelection.InAppTruckie,
        )
        assertTrue(result is RouteNavigateSession.Result.Ready)
        assertNotNull(navigateSession.activePath())
        assertNull((result as RouteNavigateSession.Result.Ready).externalHandoff)
    }

    @Test
    fun externalSelectionStillComputesPathThenOffersHandoff() {
        val flags = flagsNavigateOn()
        val navigateSession = session()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        val selection = NavigationAppSelection.External(
            packageName = "com.example.maps",
            label = "Example Maps",
        )
        when (
            val result = navigateSession.start(
                flags = flags,
                route = route,
                vehicle = vehicle,
                bufferM = 0.15,
                selection = selection,
            )
        ) {
            is RouteNavigateSession.Result.Ready -> {
                assertNotNull(navigateSession.activePath())
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
                assertTrue(result.path.points.size >= 2)
                val handoff = requireNotNull(result.externalHandoff)
                assertEquals("com.example.maps", handoff.packageName)
                assertEquals(end.latitude, handoff.destination.latitude, 0.0)
                assertEquals(end.longitude, handoff.destination.longitude, 0.0)
                assertTrue(handoff.waypoints.size >= 2)
            }
            else -> error("expected Ready with handoff, got $result")
        }
    }

    @Test
    fun startGatesFlagAndMissingEndpoints() {
        val flagsOff = LocalFeatureFlags()
        val flagsOn = flagsNavigateOn()
        val navigateSession = session()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        assertEquals(
            RouteNavigateSession.Result.FlagOff,
            navigateSession.start(
                flags = flagsOff,
                route = route,
                vehicle = vehicle,
                bufferM = 0.15,
                selection = NavigationAppSelection.InAppTruckie,
            ),
        )
        assertEquals(
            RouteNavigateSession.Result.MissingEndpoints,
            navigateSession.start(
                flags = flagsOn,
                route = Route(id = "x", dateEpochDay = 1),
                vehicle = vehicle,
                bufferM = 0.15,
                selection = NavigationAppSelection.InAppTruckie,
            ),
        )
    }

    @Test
    fun multiStopFlagOffBlocksNavigate() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        assertFalse(NavigateMultiStopCatalog.isEnabled(flags))
        val navigateSession = session()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        assertEquals(
            RouteNavigateSession.Result.FlagOff,
            navigateSession.start(
                flags = flags,
                route = route,
                vehicle = vehicle,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun missingVehicleBlocksNavigate() {
        val flags = flagsNavigateOn()
        val navigateSession = session()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        assertEquals(
            RouteNavigateSession.Result.Blocked("Active vehicle required for constrained Navigate"),
            navigateSession.start(
                flags = flags,
                route = route,
                vehicle = null,
                bufferM = 0.15,
            ),
        )
        assertNull(navigateSession.activePath())
    }

    @Test
    fun blockedPathPropagatesWithoutHandoff() {
        val flags = flagsNavigateOn()
        val blocked = object : PathRouter {
            override fun path(
                start: RouteEndpoint,
                end: RouteEndpoint,
                preferences: com.truckie1.routing.TruckRoutingPreferences,
                customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
            ): PathResult = PathResult.Blocked("no path")
        }
        val navigateSession = session(blocked)
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        val selection = NavigationAppSelection.External("com.example.maps", "Maps")
        assertEquals(
            RouteNavigateSession.Result.Blocked("no path"),
            navigateSession.start(
                flags = flags,
                route = route,
                vehicle = vehicle,
                bufferM = 0.15,
                selection = selection,
            ),
        )
        assertNull(navigateSession.activePath())
    }
}
