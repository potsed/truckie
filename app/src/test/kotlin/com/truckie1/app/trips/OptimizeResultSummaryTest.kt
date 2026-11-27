package com.truckie1.app.trips

import com.truckie1.routing.GraphHopperCustomModelStub
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.OptimizeResult
import com.truckie1.routing.RouteCandidate
import com.truckie1.routing.RoutePath
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizeResultSummaryTest {

    private val ok = OptimizeResult.Ok(
        orderedStops = listOf(
            TripStop(id = "a", addressLabel = "Depot", latitude = -27.4, longitude = 153.0),
            TripStop(id = "b", addressLabel = "Yard", latitude = -27.5, longitude = 153.1),
        ),
        route = RouteCandidate("t", 120, 12, 5.0, 40.0),
        mode = OptimizeMode.FASTEST,
        customModel = GraphHopperCustomModelStub(3.65, 12.0, 2.5, 12.0),
        path = RoutePath(
            id = "p",
            points = listOf(
                LatLngPoint(-27.4, 153.0),
                LatLngPoint(-27.5, 153.1),
            ),
            durationSeconds = 120,
            source = RoutePath.SOURCE_STUB,
        ),
    )

    @Test
    fun formatsOrderedStopLabels() {
        assertEquals("1. Depot → 2. Yard", OptimizeResultSummary.orderLabels(ok))
    }

    @Test
    fun combinedIncludesModeDurationSourceAndOrder() {
        val text = OptimizeResultSummary.combined(ok)
        assertTrue(text.contains("Fastest"))
        assertTrue(text.contains("120s"))
        assertTrue(text.contains("GraphHopper stub"))
        assertTrue(text.contains("1. Depot → 2. Yard"))
    }
}
