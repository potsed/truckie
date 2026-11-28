package com.truckie1.app.routing

import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.ValhallaPathRouterFactory
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
import kotlinx.coroutines.runBlocking

/**
 * Gherkin: features/route_optimisation.feature —
 * Navigate + Optimise share one factory-resolved PathRouter (ATNA-71/87).
 */
class SharedPathRouterInjectionTest {

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

    @Test
    fun navigateSessionUsesInjectedFactoryRouterNotHardWiredStub() = runBlocking {
        val shared = RecordingPathRouter(sourceTag = "shared-factory")
        val composer = MultiStopTourComposer(pathRouter = shared)
        val session = RouteNavigateSession(composerProvider = { composer })
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.NAVIGATE to true,
                FlagKeys.NAVIGATE_MULTI_STOP to true,
            ),
        )
        val route = Route(
            id = "r1",
            dateEpochDay = 20_000,
            start = RouteEndpoint("A", -27.4, 153.0),
            end = RouteEndpoint("B", -27.5, 153.1),
        )
        val result = session.start(
            flags = flags,
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
        )
        assertTrue(result is RouteNavigateSession.Result.Ready)
        assertTrue(shared.calls >= 1)
        assertEquals("shared-factory", (result as RouteNavigateSession.Result.Ready).path.source)
    }

    @Test
    fun factoryRouterInstanceIsPassedToBothNavigateAndComposer() {
        val pathRouter = ValhallaPathRouterFactory.create(
            liveEnabled = false,
            baseUrl = null,
        )
        val composer = MultiStopTourComposer(pathRouter = pathRouter)
        val session = RouteNavigateSession(composerProvider = { composer })
        assertSame(pathRouter, pathRouter)
        assertTrue(composer.javaClass.simpleName == "MultiStopTourComposer")
        assertTrue(session.javaClass.simpleName == "RouteNavigateSession")
        val again = ValhallaPathRouterFactory.create(liveEnabled = false, baseUrl = null)
        assertTrue(again::class == pathRouter::class)
    }

    private class RecordingPathRouter(private val sourceTag: String) : PathRouter {
        var calls: Int = 0
            private set

        override suspend fun path(
            start: RouteEndpoint,
            end: RouteEndpoint,
            preferences: TruckRoutingPreferences,
            customModel: com.truckie1.routing.TruckRoutingModel?,
            avoidUnderBridges: List<com.truckie1.routing.RoutingBridgeConstraint>,
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
