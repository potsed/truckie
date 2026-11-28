package com.truckie1.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation.feature —
 * shared default truck model (ATNA-71); no duplicated 4.0 m / 12 t literals.
 */
class TruckRoutingModelDefaultTest {

    @Test
    fun defaultUsesSharedClearanceMassWidthLength() {
        val model = TruckRoutingModel.default()
        assertEquals(4.0, model.requiredClearanceHeightM, 0.0)
        assertEquals(12.0, model.requiredMassT, 0.0)
        assertEquals(2.5, model.vehicleWidthM, 0.0)
        assertEquals(12.0, model.vehicleLengthM, 0.0)
        assertEquals(TruckRoutingPreferences.DEFAULT, model.preferences)
    }

    @Test
    fun stubPathRouterUsesSharedDefaultWhenModelNull() = runBlocking {
        val router = StubPathRouter()
        val start = com.truckie1.trips.RouteEndpoint("A", -27.4, 153.0)
        val end = com.truckie1.trips.RouteEndpoint("B", -27.5, 153.1)
        when (val result = router.path(start, end, customModel = null)) {
            is PathResult.Ok -> {
                val snippet = requireNotNull(result.path.customModelSnippet)
                assertTrue(snippet.contains("4.0"))
                assertTrue(snippet.contains("12.0"))
                assertEquals(
                    TruckRoutingModel.default().toRequestSnippet(),
                    snippet,
                )
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }

    @Test
    fun liveValhallaPathRouterUsesSharedDefaultWhenModelNull() = runBlocking {
        val json = """
            {
              "trip": {
                "summary": { "time": 60.0 },
                "legs": [{
                  "shape": "_p~iF~ps|U_ulLnnqC"
                }]
              }
            }
        """.trimIndent()
        val router = LiveValhallaPathRouter(
            baseUrl = "https://vh.example",
            httpClient = { _, body, _ ->
                assertTrue(body.contains("\"costing\": \"truck\""))
                assertTrue(body.contains("\"height\": 4.0"))
                assertTrue(body.contains("\"weight\": 12.0"))
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
