package com.truckie1.trips

import java.time.LocalDate
import java.util.UUID

/**
 * Active [Route] store for the map-first home drag sheet.
 * Real and demo modes share this contract — only the backing dataset differs.
 */
interface RouteRepository {
    fun activeRoute(): Route?

    fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate = LocalDate.now(),
        customDate: LocalDate? = null,
        start: RouteEndpoint? = null,
        end: RouteEndpoint? = null,
    ): Route

    fun addStop(stop: RouteStop)
    fun removeStop(id: String)
    fun clear()

    /** Replaces the active route in one shot. Pass null to clear. */
    fun replaceActive(route: Route?)
}

/**
 * In-memory editable active [Route] for the map-first home drag sheet.
 */
class RouteSession : RouteRepository {
    private var active: Route? = null

    override fun activeRoute(): Route? = active

    override fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate,
        customDate: LocalDate?,
        start: RouteEndpoint?,
        end: RouteEndpoint?,
    ): Route {
        val date = when (dateChoice) {
            RouteDateChoice.TODAY -> today
            RouteDateChoice.TOMORROW -> today.plusDays(1)
            RouteDateChoice.CUSTOM -> requireNotNull(customDate) {
                "customDate required when dateChoice is CUSTOM"
            }
        }
        val route = Route(
            id = UUID.randomUUID().toString(),
            name = name?.trim()?.takeIf { it.isNotEmpty() },
            dateEpochDay = date.toEpochDay(),
            status = RouteStatus.DRAFT,
            start = start,
            end = end,
            stops = emptyList(),
        )
        active = route
        return route
    }

    override fun addStop(stop: RouteStop) {
        val route = requireNotNull(active) { "Create a route before adding stops" }
        require(route.stops.none { it.id == stop.id }) { "Duplicate stop id: ${stop.id}" }
        val ordered = stop.copy(order = route.stops.size)
        active = route.copy(stops = route.stops + ordered, status = RouteStatus.ACTIVE)
    }

    override fun removeStop(id: String) {
        val route = active ?: return
        val remaining = route.stops.filterNot { it.id == id }
            .mapIndexed { index, stop -> stop.copy(order = index) }
        active = route.copy(stops = remaining)
    }

    override fun clear() {
        active = null
    }

    override fun replaceActive(route: Route?) {
        active = route
    }
}
