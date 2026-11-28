package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/route_follow_roads.feature — stub is colinear / degraded. */
class StubPathIsNotRoadNetworkTest {

    @Test
    fun stubInterpolateIsColinearAndNotRoadNetwork() = runBlocking {
        val router = StubGraphHopperPathRouter(intermediatePoints = 4)
        val start = RouteEndpoint("A", -27.40, 153.00)
        val end = RouteEndpoint("B", -27.50, 153.10)
        when (val result = router.path(start, end)) {
            is PathResult.Ok -> {
                assertEquals(RoutePath.SOURCE_STUB, result.path.source)
                assertFalse(result.path.followsRoadNetwork)
                val points = result.path.points
                assertTrue(points.size >= 3)
                for (p in points) {
                    val tLat = (p.latitude - start.latitude) / (end.latitude - start.latitude)
                    val tLng = (p.longitude - start.longitude) / (end.longitude - start.longitude)
                    assertEquals(tLat, tLng, 1e-9)
                }
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }
}
