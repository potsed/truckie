package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/path_block_detour.feature — PathRouter alternates (ATNA-102).
 */
class PathBlockDetourTest {

    private val underLow = RoutingBridgeConstraint(
        id = "low-under",
        latitude = -27.425,
        longitude = 153.105,
        clearanceHeightM = 1.9,
        passage = RoutingBridgePassage.UNDER,
    )

    @Test
    fun catalogPrefersPathRouterNotHandRolledPolyline() {
        assertTrue(PathBlockDetour.usesPathRouter)
        assertFalse(PathBlockDetour.usesHandRolledPolylineRule)
        assertTrue(PathBlockDetour.prefersLiveGraphHopper)
    }

    @Test
    fun resolveLegRetriesPathRouterWithAvoidAndKeepsDrawablePoints() = runBlocking {
        val router = StubGraphHopperPathRouter(intermediatePoints = 8)
        val start = RouteEndpoint("Depot", -27.42, 153.10)
        val end = RouteEndpoint("Near", -27.43, 153.11)
        val chord = listOf(
            LatLngPoint(start.latitude, start.longitude),
            LatLngPoint(underLow.latitude, underLow.longitude),
            LatLngPoint(end.latitude, end.longitude),
        )
        assertTrue(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = chord,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
        val result = PathBlockDetour.resolveLeg(
            pathRouter = router,
            start = start,
            end = end,
            preferences = TruckRoutingPreferences.DEFAULT,
            customModel = GraphHopperCustomModelStub.default(),
            bridges = listOf(underLow),
            vehicleHeightM = 3.5,
            bufferM = 0.15,
            bridgePassageEnabled = true,
        )
        assertTrue(result is PathResult.Ok)
        val path = (result as PathResult.Ok).path
        assertTrue(path.points.size >= 2)
        assertTrue(path.id.contains("detour") || path.points.size >= 2)
        assertFalse(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = path.points,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                allowedNearPoints = listOf(
                    LatLngPoint(start.latitude, start.longitude),
                    LatLngPoint(end.latitude, end.longitude),
                ),
            ),
        )
    }

    @Test
    fun clearLegDoesNotForceAvoidRetry() = runBlocking {
        val router = StubGraphHopperPathRouter()
        val start = RouteEndpoint("A", -27.42, 153.10)
        val end = RouteEndpoint("B", -27.421, 153.101)
        val result = PathBlockDetour.resolveLeg(
            pathRouter = router,
            start = start,
            end = end,
            preferences = TruckRoutingPreferences.DEFAULT,
            customModel = null,
            bridges = emptyList(),
            vehicleHeightM = 3.5,
            bufferM = 0.15,
            bridgePassageEnabled = true,
        )
        assertTrue(result is PathResult.Ok)
        assertEquals(RoutePath.SOURCE_STUB, (result as PathResult.Ok).path.source)
        assertFalse(result.path.id.contains("detour"))
    }
}
