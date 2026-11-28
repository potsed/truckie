package com.truckie1.app.profiles

import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure mapper coverage for Room route/stop entities (ATNA-89).
 */
class RouteEntityMapperTest {

    @Test
    fun roundTripsRouteWithStopsAndCargo() {
        val route = Route(
            id = "r1",
            name = "Airport run",
            dateEpochDay = 20_450L,
            status = RouteStatus.ACTIVE,
            start = RouteEndpoint("Depot", -27.4, 153.0),
            end = RouteEndpoint("Depot", -27.4, 153.0),
            stops = listOf(
                RouteStop(
                    id = "s1",
                    addressLabel = "Yard",
                    latitude = -27.5,
                    longitude = 153.1,
                    kind = StopKind.PICKUP_LOAD,
                    order = 0,
                    cargo = listOf(
                        Cargo("Crates", 1.2, 1.0, 1.5, 0.5, CargoAction.PICK_UP),
                    ),
                ),
                RouteStop(
                    id = "s2",
                    addressLabel = "Customer",
                    latitude = -27.6,
                    longitude = 153.2,
                    kind = StopKind.DROP_OFF_UNLOAD,
                    order = 1,
                ),
            ),
        )

        val entity = RouteEntityMapper.toRouteEntity(route)
        val stops = RouteEntityMapper.toStopEntities(route.id, route.stops)
        val cargo = RouteEntityMapper.toCargoEntities(route.stops)
        val restored = RouteEntityMapper.toDomainRoute(
            entity = entity,
            stopEntities = stops,
            cargoByStopId = cargo.groupBy { it.stopId },
        )

        assertEquals(route, restored)
        assertEquals("ACTIVE", entity.status)
        assertEquals(2, stops.size)
        assertEquals(1, cargo.size)
    }

    @Test
    fun draftWithoutEndpointsMapsNullColumns() {
        val route = Route(
            id = "draft",
            name = null,
            dateEpochDay = 1L,
            status = RouteStatus.DRAFT,
        )
        val entity = RouteEntityMapper.toRouteEntity(route)
        assertNull(entity.startAddress)
        assertNull(entity.endLatitude)
        val restored = RouteEntityMapper.toDomainRoute(entity, emptyList(), emptyMap())
        assertNull(restored.start)
        assertNull(restored.end)
        assertTrue(restored.stops.isEmpty())
    }

    @Test
    fun optimizeModeParsing() {
        assertEquals(OptimizeMode.FASTEST, RouteEntityMapper.optimizeModeOrNull("FASTEST"))
        assertNull(RouteEntityMapper.optimizeModeOrNull(null))
        assertNull(RouteEntityMapper.optimizeModeOrNull("NOPE"))
    }
}
