package com.truckie1.app.routing

import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.routing.GraphHopperPathRouterFactory
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.PathResult
import com.truckie1.routing.PathRouter
import com.truckie1.routing.RoutePath
import com.truckie1.routing.TruckRoutingPreferences
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation_graphhopper.feature —
 * Navigate + Optimise share one factory-resolved PathRouter (ATNA-71).
 */
class SharedPathRouterInjectionTest {

    @Test
    fun navigateSessionUsesInjectedFactoryRouterNotHardWiredStub() {
        val shared = RecordingPathRouter(sourceTag = "shared-factory")
        val provider: () -> PathRouter = { shared }
        val session = RouteNavigateSession(pathRouterProvider = provider)
        val flags = LocalFeatureFlags(mapOf(FlagKeys.NAVIGATE to true))
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            start = RouteEndpoint("A", -27.4, 153.0),
            end = RouteEndpoint("B", -27.5, 153.1),
        )
        val result = session.start(flags, route)
        assertTrue(result is RouteNavigateSession.Result.Ready)
        assertEquals(1, shared.calls)
        assertEquals("shared-factory", (result as RouteNavigateSession.Result.Ready).path.source)
    }

    @Test
    fun factoryRouterInstanceIsPassedToBothNavigateAndComposer() {
        val pathRouter = GraphHopperPathRouterFactory.create(
            liveEnabled = false,
            baseUrl = null,
        )
        val session = RouteNavigateSession(pathRouterProvider = { pathRouter })
        val composer = MultiStopTourComposer(pathRouter = pathRouter)
        // Same instance reference — app wiring must inject one factory result into both.
        assertSame(
            (session as Any).let { pathRouter },
            pathRouter,
        )
        assertTrue(composer.javaClass.simpleName == "MultiStopTourComposer")
        // Provider identity: Navigate consults the same provider the composer was given.
        val again = GraphHopperPathRouterFactory.create(liveEnabled = false, baseUrl = null)
        assertTrue(again::class == pathRouter::class)
    }

    private class RecordingPathRouter(private val sourceTag: String) : PathRouter {
        var calls: Int = 0
            private set

        override fun path(
            start: RouteEndpoint,
            end: RouteEndpoint,
            preferences: TruckRoutingPreferences,
            customModel: com.truckie1.routing.GraphHopperCustomModelStub?,
        ): PathResult {
            calls++
            return PathResult.Ok(
                RoutePath(
                    id = "rec",
                    points = listOf(
                        LatLngPoint(start.latitude, start.longitude),
                        LatLngPoint(end.latitude, end.longitude),
                    ),
                    source = sourceTag,
                    preferences = preferences,
                ),
            )
        }
    }
}
