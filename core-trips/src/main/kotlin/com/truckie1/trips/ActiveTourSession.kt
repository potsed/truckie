package com.truckie1.trips

import java.time.LocalDate
import java.util.UUID

/**
 * Single active-tour facade for map-home [Route] + Plan Trip / Optimise
 * [TripStop] projection (`ff_active_tour`, ATNA-86 / P1-1).
 *
 * [Route] is the editable source of truth. [TripRepository] methods project
 * through [TripStopProjection] so Optimise and clearance helpers keep their
 * existing [TripStop] surface without a parallel stop list.
 *
 * Gherkin: features/active_tour.feature
 */
class ActiveTourSession : RouteRepository, TripRepository {
    private var active: Route? = null

    override var lastOptimizeMode: OptimizeMode? = null

    // --- RouteRepository ---

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
        lastOptimizeMode = null
    }

    override fun replaceActive(route: Route?) {
        active = route
        if (route == null) {
            lastOptimizeMode = null
        }
    }

    // --- TripRepository (projection over active Route.stops) ---

    override fun stops(): List<TripStop> =
        TripStopProjection.toTripStops(active?.stops.orEmpty())

    override fun addStop(stop: TripStop) {
        ensureDraftRoute()
        val route = requireNotNull(active)
        require(route.stops.none { it.id == stop.id }) { "Duplicate stop id: ${stop.id}" }
        addStop(
            TripStopProjection.toRouteStop(
                stop = stop,
                order = route.stops.size,
            ),
        )
    }

    override fun moveStop(fromIndex: Int, toIndex: Int) {
        val route = active ?: return
        val current = route.stops.toMutableList()
        require(fromIndex in current.indices) { "fromIndex out of range" }
        require(toIndex in current.indices) { "toIndex out of range" }
        if (fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        active = route.copy(
            stops = current.mapIndexed { index, stop -> stop.copy(order = index) },
        )
    }

    override fun replaceStops(newStops: List<TripStop>) {
        ensureDraftRoute()
        val previous = active?.stops.orEmpty()
        val restored = TripStopProjection.toRouteStops(newStops, previous = previous)
        active = active!!.copy(
            stops = restored,
            status = if (restored.isEmpty()) RouteStatus.DRAFT else RouteStatus.ACTIVE,
        )
    }

    override fun updateStop(stop: TripStop) {
        val route = requireNotNull(active) { "Create a route before updating stops" }
        val index = route.stops.indexOfFirst { it.id == stop.id }
        require(index >= 0) { "Unknown stop id: ${stop.id}" }
        val previous = route.stops[index]
        val updated = TripStopProjection.toRouteStop(
            stop = stop,
            order = previous.order,
            kind = previous.kind,
        )
        val next = route.stops.toMutableList()
        next[index] = updated
        active = route.copy(stops = next)
    }

    private fun ensureDraftRoute() {
        if (active != null) return
        createRoute(
            name = null,
            dateChoice = RouteDateChoice.TODAY,
            today = LocalDate.now(),
            customDate = null,
            start = null,
            end = null,
        )
    }
}
