package com.truckie1.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation_graphhopper.feature —
 * shared default Custom Model (ATNA-71); no duplicated 4.0 m / 12 t literals.
 */
class GraphHopperCustomModelDefaultTest {

    @Test
    fun defaultUsesSharedClearanceMassWidthLength() {
        val model = GraphHopperCustomModelStub.default()
        assertEquals(4.0, model.requiredClearanceHeightM, 0.0)
        assertEquals(12.0, model.requiredMassT, 0.0)
        assertEquals(2.5, model.vehicleWidthM, 0.0)
        assertEquals(12.0, model.vehicleLengthM, 0.0)
        assertEquals(TruckRoutingPreferences.DEFAULT, model.preferences)
    }

    @Test
    fun stubPathRouterUsesSharedDefaultWhenModelNull() = runBlocking {
        val router = StubGraphHopperPathRouter()
        val start = com.truckie1.trips.RouteEndpoint("A", -27.4, 153.0)
        val end = com.truckie1.trips.RouteEndpoint("B", -27.5, 153.1)
        when (val result = router.path(start, end, customModel = null)) {
            is PathResult.Ok -> {
                val snippet = requireNotNull(result.path.customModelSnippet)
                assertTrue(snippet.contains("4.0"))
                assertTrue(snippet.contains("12.0"))
                assertEquals(
                    GraphHopperCustomModelStub.default().toRequestSnippet(),
                    snippet,
                )
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }

    @Test
    fun livePathRouterUsesSharedDefaultWhenModelNull() = runBlocking {
        val json = """
            {
              "paths": [{
                "distance": 100.0,
                "time": 60000,
                "points": { "coordinates": [[153.0, -27.4], [153.1, -27.5]] }
              }]
            }
        """.trimIndent()
        val router = LiveGraphHopperPathRouter(
            baseUrl = "https://gh.example",
            httpClient = { _, body ->
                assertTrue(body.contains(GraphHopperCustomModelStub.default().toRequestSnippet()))
                json
            },
        )
        val result = router.path(
            com.truckie1.trips.RouteEndpoint("A", -27.4, 153.0),
            com.truckie1.trips.RouteEndpoint("B", -27.5, 153.1),
            customModel = null,
        )
        assertTrue(result is PathResult.Ok)
    }
}
