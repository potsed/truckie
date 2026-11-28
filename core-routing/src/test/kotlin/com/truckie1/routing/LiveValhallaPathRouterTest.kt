package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/route_follow_roads.feature — hosted Valhalla PathRouter. */
class LiveValhallaPathRouterTest {

    @Test
    fun parsesTripShapeAndUsesTruckCostingWithBearer() = runBlocking {
        // Manually built polyline6 for two nearby points is awkward; inject
        // a known precision-5 sample decoded with precision 6 will be wrong —
        // instead put geo-ish shape by encoding via the inverse of a known path:
        // Use a minimal valid Valhalla-like response with a short shape.
        // Encode (-27.42, 153.10) -> (-27.43, 153.11) at precision 6.
        val shape = encodePolyline6(
            listOf(
                LatLngPoint(-27.42, 153.10),
                LatLngPoint(-27.425, 153.105),
                LatLngPoint(-27.43, 153.11),
            ),
        )
        var capturedUrl = ""
        var capturedBody = ""
        var capturedBearer: String? = null
        val json = """
            {
              "trip": {
                "legs": [{
                  "summary": { "time": 180.0, "length": 1.2 },
                  "shape": "$shape"
                }],
                "summary": { "time": 180.0, "length": 1.2 }
              }
            }
        """.trimIndent()
        val router = LiveValhallaPathRouter(
            baseUrl = "https://api.simplerouting.io/valhalla",
            apiKey = "test-key",
            httpClient = { url, body, bearer ->
                capturedUrl = url
                capturedBody = body
                capturedBearer = bearer
                json
            },
        )
        when (
            val result = router.path(
                RouteEndpoint("A", -27.42, 153.10),
                RouteEndpoint("B", -27.43, 153.11),
            )
        ) {
            is PathResult.Ok -> {
                assertEquals(RoutePath.SOURCE_VALHALLA, result.path.source)
                assertTrue(result.path.followsRoadNetwork)
                assertEquals(3, result.path.points.size)
                assertEquals(-27.42, result.path.points[0].latitude, 1e-5)
                assertEquals(153.10, result.path.points[0].longitude, 1e-5)
                assertEquals(180L, result.path.durationSeconds)
                assertEquals("https://api.simplerouting.io/valhalla/route", capturedUrl)
                assertEquals("test-key", capturedBearer)
                assertTrue(capturedBody.contains("\"costing\": \"truck\""))
                assertTrue(capturedBody.contains("\"height\""))
                assertFalse(capturedBody.contains("residential_soft_preference_only"))
            }
            is PathResult.Blocked -> error(result.reason)
        }
    }

    @Test
    fun blocksWhenTripMissing() = runBlocking {
        val router = LiveValhallaPathRouter(
            baseUrl = "https://vh.example",
            httpClient = { _, _, _ -> """{"error":"No path found","error_code":442}""" },
        )
        val result = router.path(
            RouteEndpoint("A", -27.42, 153.10),
            RouteEndpoint("B", -27.43, 153.11),
        )
        assertTrue(result is PathResult.Blocked)
    }

    private fun encodePolyline6(points: List<LatLngPoint>): String {
        val factor = 1e6
        var prevLat = 0
        var prevLng = 0
        val sb = StringBuilder()
        for (p in points) {
            val lat = Math.round(p.latitude * factor).toInt()
            val lng = Math.round(p.longitude * factor).toInt()
            sb.append(encodeSigned(lat - prevLat))
            sb.append(encodeSigned(lng - prevLng))
            prevLat = lat
            prevLng = lng
        }
        return sb.toString()
    }

    private fun encodeSigned(value: Int): String {
        var v = if (value < 0) (value shl 1).inv() else (value shl 1)
        val out = StringBuilder()
        while (v >= 0x20) {
            out.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        out.append((v + 63).toChar())
        return out.toString()
    }
}
