package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Live GraphHopper HTTP client parses route geometry; transport is injected.
 */
class LiveGraphHopperPathRouterTest {

    @Test
    fun parsesPointsFromGraphHopperJson() = runBlocking {
        val json = """
            {
              "paths": [{
                "distance": 1200.0,
                "time": 180000,
                "points": {
                  "coordinates": [
                    [153.10, -27.42],
                    [153.11, -27.43]
                  ]
                }
              }]
            }
        """.trimIndent()
        val router = LiveGraphHopperPathRouter(
            baseUrl = "https://gh.example",
            httpClient = { _, _ -> json },
        )
        val start = RouteEndpoint("A", -27.42, 153.10)
        val end = RouteEndpoint("B", -27.43, 153.11)
        when (val result = router.path(start, end)) {
            is PathResult.Ok -> {
                assertEquals(RoutePath.SOURCE_LIVE, result.path.source)
                assertEquals(2, result.path.points.size)
                assertEquals(-27.42, result.path.points[0].latitude, 1e-6)
                assertEquals(153.10, result.path.points[0].longitude, 1e-6)
                assertEquals(180L, result.path.durationSeconds)
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }

    @Test
    fun blocksWhenResponseHasNoPaths() = runBlocking {
        val router = LiveGraphHopperPathRouter(
            baseUrl = "https://gh.example",
            httpClient = { _, _ -> """{"paths":[]}""" },
        )
        val result = router.path(
            RouteEndpoint("A", -27.42, 153.10),
            RouteEndpoint("B", -27.43, 153.11),
        )
        assertTrue(result is PathResult.Blocked)
    }
}
