package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature — GraphHopper stub path for Navigate.
 */
class StubGraphHopperPathRouterTest {

    private val router = StubGraphHopperPathRouter()

    @Test
    fun oneWayPathIncludesStartAndEnd() {
        val start = RouteEndpoint("Depot", -27.45, 153.01)
        val end = RouteEndpoint("Customer", -27.47, 153.03)
        when (val result = router.path(start, end)) {
            is PathResult.Ok -> {
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
                assertTrue(result.path.points.size >= 2)
                assertEquals(start.latitude, result.path.points.first().latitude, 0.0)
                assertEquals(start.longitude, result.path.points.first().longitude, 0.0)
                assertEquals(end.latitude, result.path.points.last().latitude, 0.0)
                assertEquals(end.longitude, result.path.points.last().longitude, 0.0)
                assertTrue(result.path.preferences.preferNonResidentialThroughRoutes)
                assertTrue(result.path.customModelSnippet!!.contains("residential_soft_preference_only"))
                assertTrue(result.path.customModelSnippet!!.contains("RESIDENTIAL"))

            }
            is PathResult.Blocked -> error(result.reason)
        }
    }

    @Test
    fun roundTripReturnsClosedLoopGeometry() {
        val place = RouteEndpoint("Home", -27.5, 153.0)
        when (val result = router.path(place, place)) {
            is PathResult.Ok -> {
                assertTrue(result.path.points.size >= 3)
                assertEquals(place.latitude, result.path.points.first().latitude, 0.0)
                assertEquals(place.latitude, result.path.points.last().latitude, 0.0)
                assertEquals(place.longitude, result.path.points.first().longitude, 0.0)
                assertEquals(place.longitude, result.path.points.last().longitude, 0.0)
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }
}
