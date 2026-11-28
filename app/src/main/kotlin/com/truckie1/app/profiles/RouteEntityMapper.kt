package com.truckie1.app.profiles

import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStatus
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind

/**
 * Maps domain [Route] / [RouteStop] / [Cargo] ↔ Room entities (ATNA-89).
 * Pure functions — unit-tested without Room.
 */
object RouteEntityMapper {

    fun toRouteEntity(route: Route): RouteEntity = RouteEntity(
        id = route.id,
        name = route.name,
        dateEpochDay = route.dateEpochDay,
        status = route.status.name,
        startAddress = route.start?.addressLabel,
        startLatitude = route.start?.latitude,
        startLongitude = route.start?.longitude,
        endAddress = route.end?.addressLabel,
        endLatitude = route.end?.latitude,
        endLongitude = route.end?.longitude,
    )

    fun toStopEntities(routeId: String, stops: List<RouteStop>): List<RouteStopEntity> =
        stops.map { stop ->
            RouteStopEntity(
                id = stop.id,
                routeId = routeId,
                addressLabel = stop.addressLabel,
                latitude = stop.latitude,
                longitude = stop.longitude,
                kind = stop.kind.name,
                stopOrder = stop.order,
            )
        }

    fun toCargoEntities(stops: List<RouteStop>): List<RouteCargoEntity> =
        stops.flatMap { stop ->
            stop.cargo.mapIndexed { index, cargo ->
                RouteCargoEntity(
                    stopId = stop.id,
                    description = cargo.description,
                    heightM = cargo.heightM,
                    widthM = cargo.widthM,
                    lengthM = cargo.lengthM,
                    massT = cargo.massT,
                    action = cargo.action.name,
                    cargoOrder = index,
                )
            }
        }

    fun toDomainRoute(
        entity: RouteEntity,
        stopEntities: List<RouteStopEntity>,
        cargoByStopId: Map<String, List<RouteCargoEntity>>,
    ): Route = Route(
        id = entity.id,
        name = entity.name,
        dateEpochDay = entity.dateEpochDay,
        status = RouteStatus.valueOf(entity.status),
        start = endpointOrNull(
            entity.startAddress,
            entity.startLatitude,
            entity.startLongitude,
        ),
        end = endpointOrNull(
            entity.endAddress,
            entity.endLatitude,
            entity.endLongitude,
        ),
        stops = stopEntities.sortedBy { it.stopOrder }.map { stop ->
            RouteStop(
                id = stop.id,
                addressLabel = stop.addressLabel,
                latitude = stop.latitude,
                longitude = stop.longitude,
                kind = StopKind.valueOf(stop.kind),
                order = stop.stopOrder,
                cargo = (cargoByStopId[stop.id].orEmpty())
                    .sortedBy { it.cargoOrder }
                    .map { cargo ->
                        Cargo(
                            description = cargo.description,
                            heightM = cargo.heightM,
                            widthM = cargo.widthM,
                            lengthM = cargo.lengthM,
                            massT = cargo.massT,
                            action = CargoAction.valueOf(cargo.action),
                        )
                    },
            )
        },
    )

    fun optimizeModeOrNull(raw: String?): OptimizeMode? =
        raw?.let { runCatching { OptimizeMode.valueOf(it) }.getOrNull() }

    private fun endpointOrNull(
        address: String?,
        latitude: Double?,
        longitude: Double?,
    ): RouteEndpoint? {
        if (address.isNullOrBlank() || latitude == null || longitude == null) return null
        return RouteEndpoint(addressLabel = address, latitude = latitude, longitude = longitude)
    }
}
