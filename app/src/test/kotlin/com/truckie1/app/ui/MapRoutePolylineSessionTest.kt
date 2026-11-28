package com.truckie1.app.ui

import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Gherkin: features/async_routing_io.feature — polyline session cache.
 */
class MapRoutePolylineSessionTest {

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

    private val route = Route(
        id = "r1",
        name = "Run",
        dateEpochDay = 20_000L,
        status = RouteStatus.ACTIVE,
        start = RouteEndpoint("A", -27.4, 153.0),
        end = RouteEndpoint("B", -27.5, 153.1),
    )

    @Test
    fun remembersOptimisedAndPreviewByKey() {
        val session = MapRoutePolylineSession()
        val path = RoutePath(
            id = "p1",
            points = listOf(
                LatLngPoint(-27.4, 153.0),
                LatLngPoint(-27.5, 153.1),
            ),
        )
        session.rememberOptimised(path)
        assertEquals(path, session.lastOptimisedPath())

        val key = session.previewCacheKey(
            route = route,
            vehicle = vehicle,
            bufferM = 0.15,
            bridgePassage = true,
            valhallaLive = false,
        )
        session.rememberPreview(key, path)
        assertEquals(path, session.cachedPreview(key))
        assertNull(session.cachedPreview(key + "-other"))

        val otherKey = session.previewCacheKey(
            route = route,
            vehicle = vehicle,
            bufferM = 0.3,
            bridgePassage = true,
            valhallaLive = false,
        )
        assertNotEquals(key, otherKey)

        session.clear()
        assertNull(session.lastOptimisedPath())
        assertNull(session.cachedPreview(key))
    }
}
