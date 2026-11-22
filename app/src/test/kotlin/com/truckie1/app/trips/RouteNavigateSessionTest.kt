package com.truckie1.app.trips

import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
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
 * Gherkin: create_route_start_end.feature — in-app GraphHopper Navigate + optional handoff.
 */
class RouteNavigateSessionTest {

    private val start = RouteEndpoint("Depot", -27.45, 153.01)
    private val end = RouteEndpoint("Customer", -27.47, 153.03)

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
    fun canNavigateRequiresFlagAndEndpoints() {
        val flagsOff = LocalFeatureFlags()
        val flagsOn = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val ready = Route(
            id = "r1",
            dateEpochDay = 20_000,
            status = RouteStatus.ACTIVE,
            start = start,
            end = end,
        )
        assertFalse(RouteNavigateCatalog.canNavigate(flagsOff, ready))
        assertFalse(RouteNavigateCatalog.canNavigate(flagsOn, null))
        assertFalse(
            RouteNavigateCatalog.canNavigate(
                flagsOn,
                Route(id = "x", dateEpochDay = 1),
            ),
        )
        assertTrue(RouteNavigateCatalog.canNavigate(flagsOn, ready))
        assertTrue(RouteNavigateCatalog.canNavigate(flagsOn, ready.copy(end = start)))
    }

    @Test
    fun startAlwaysComputesInAppGraphHopperPath() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val session = RouteNavigateSession(StubGraphHopperPathRouter())
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            start = start,
            end = end,
        )
        when (val result = session.start(flags, route, NavigationAppSelection.InAppTruckie)) {
            is RouteNavigateSession.Result.Ready -> {
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
                assertTrue(result.path.points.size >= 2)
                assertNull(result.externalHandoff)
                assertEquals(result.path, session.activePath())
            }
            else -> error("expected Ready, got $result")
        }
    }

    @Test
    fun startRoundTripStillYieldsInAppPath() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val session = RouteNavigateSession(StubGraphHopperPathRouter())
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = start)
        val result = session.start(flags, route, NavigationAppSelection.InAppTruckie)
        assertTrue(result is RouteNavigateSession.Result.Ready)
        assertNotNull(session.activePath())
        assertNull((result as RouteNavigateSession.Result.Ready).externalHandoff)
    }

    @Test
    fun externalSelectionStillComputesPathThenOffersHandoff() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val session = RouteNavigateSession(StubGraphHopperPathRouter())
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        val selection = NavigationAppSelection.External(
            packageName = "com.example.maps",
            label = "Example Maps",
        )
        when (val result = session.start(flags, route, selection)) {
            is RouteNavigateSession.Result.Ready -> {
                // GraphHopper path always calculated and held for the map.
                assertNotNull(session.activePath())
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
        val flagsOn = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val session = RouteNavigateSession()
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        assertEquals(
            RouteNavigateSession.Result.FlagOff,
            session.start(flagsOff, route, NavigationAppSelection.InAppTruckie),
        )
        assertEquals(
            RouteNavigateSession.Result.MissingEndpoints,
            session.start(
                flagsOn,
                Route(id = "x", dateEpochDay = 1),
                NavigationAppSelection.InAppTruckie,
            ),
        )
    }

    @Test
    fun blockedPathPropagatesWithoutHandoff() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val blocked = object : PathRouter {
            override fun path(
                start: com.truckie1.trips.RouteEndpoint,
                end: com.truckie1.trips.RouteEndpoint,
                preferences: com.truckie1.routing.TruckRoutingPreferences,
                customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
            ): PathResult = PathResult.Blocked("no path")
        }
        val session = RouteNavigateSession(blocked)
        val route = Route(id = "r1", dateEpochDay = 20_000, start = start, end = end)
        val selection = NavigationAppSelection.External("com.example.maps", "Maps")
        assertEquals(
            RouteNavigateSession.Result.Blocked("no path"),
            session.start(flags, route, selection),
        )
        assertNull(session.activePath())
    }
}
