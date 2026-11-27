package com.truckie1.trips

/**
 * Projects [RouteStop] ↔ [TripStop] so Optimise / clearance / mass keep using
 * [TripStop] while [Route] remains the single editable source of truth
 * (`ff_active_tour`, ATNA-86).
 *
 * Gherkin: features/active_tour.feature
 */
object TripStopProjection {

    fun toTripStop(stop: RouteStop): TripStop = TripStop(
        id = stop.id,
        addressLabel = stop.addressLabel,
        latitude = stop.latitude,
        longitude = stop.longitude,
        cargo = stop.cargo,
    )

    fun toTripStops(stops: List<RouteStop>): List<TripStop> =
        stops.map(::toTripStop)

    fun toRouteStop(
        stop: TripStop,
        order: Int,
        kind: StopKind = inferKind(stop),
    ): RouteStop = RouteStop(
        id = stop.id,
        addressLabel = stop.addressLabel,
        latitude = stop.latitude,
        longitude = stop.longitude,
        kind = kind,
        order = order,
        cargo = stop.cargo,
    )

    /**
     * Rebuilds ordered [RouteStop]s from a [TripStop] list, preserving [StopKind]
     * from [previous] when ids match; otherwise infers kind from cargo actions.
     */
    fun toRouteStops(
        stops: List<TripStop>,
        previous: List<RouteStop> = emptyList(),
    ): List<RouteStop> {
        val kindById = previous.associate { it.id to it.kind }
        return stops.mapIndexed { index, stop ->
            toRouteStop(
                stop = stop,
                order = index,
                kind = kindById[stop.id] ?: inferKind(stop),
            )
        }
    }

    fun inferKind(stop: TripStop): StopKind {
        val hasPickup = stop.cargo.any { it.action == CargoAction.PICK_UP }
        val hasDrop = stop.cargo.any { it.action == CargoAction.DROP_OFF }
        return when {
            hasPickup && !hasDrop -> StopKind.PICKUP_LOAD
            else -> StopKind.DROP_OFF_UNLOAD
        }
    }
}
