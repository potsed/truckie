package com.truckie1.trips

import java.time.LocalDate
import java.util.UUID

/**
 * In-memory editable active [Route] for the map-first home drag sheet.
 */
class RouteSession {
    private var active: Route? = null

    fun activeRoute(): Route? = active

    fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate = LocalDate.now(),
        customDate: LocalDate? = null,
        start: RouteEndpoint? = null,
        end: RouteEndpoint? = null,
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

    fun addStop(stop: RouteStop) {
        val route = requireNotNull(active) { "Create a route before adding stops" }
        require(route.stops.none { it.id == stop.id }) { "Duplicate stop id: ${stop.id}" }
        val ordered = stop.copy(order = route.stops.size)
        active = route.copy(stops = route.stops + ordered, status = RouteStatus.ACTIVE)
    }

    fun removeStop(id: String) {
        val route = active ?: return
        val remaining = route.stops.filterNot { it.id == id }
            .mapIndexed { index, stop -> stop.copy(order = index) }
        active = route.copy(stops = remaining)
    }

    fun clear() {
        active = null
    }

    /**
     * Replaces the active route in one shot (used by Demo mode isolation).
     * Pass null to clear without creating a new route.
     */
    fun replaceActive(route: Route?) {
        active = route
    }
}
