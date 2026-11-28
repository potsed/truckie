package com.truckie1.app.profiles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route

/**
 * Suspend Room DAO for the active tour (ATNA-89 / P1-4).
 * Never call from the main thread without hopping to IO.
 */
@Dao
interface ActiveTourDao {
    @Query("SELECT * FROM active_tour_meta WHERE id = 1 LIMIT 1")
    suspend fun meta(): ActiveTourMetaEntity?

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun routeById(id: String): RouteEntity?

    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY stopOrder ASC")
    suspend fun stopsForRoute(routeId: String): List<RouteStopEntity>

    @Query("SELECT * FROM route_cargo WHERE stopId IN (:stopIds) ORDER BY cargoOrder ASC")
    suspend fun cargoForStops(stopIds: List<String>): List<RouteCargoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStops(stops: List<RouteStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCargo(cargo: List<RouteCargoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: ActiveTourMetaEntity)

    @Query("DELETE FROM route_cargo")
    suspend fun deleteAllCargo()

    @Query("DELETE FROM route_stops")
    suspend fun deleteAllStops()

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Transaction
    suspend fun loadActiveTour(): ActiveTourSnapshot {
        val meta = meta() ?: return ActiveTourSnapshot(route = null, lastOptimizeMode = null)
        val mode = RouteEntityMapper.optimizeModeOrNull(meta.lastOptimizeMode)
        val routeId = meta.activeRouteId ?: return ActiveTourSnapshot(route = null, lastOptimizeMode = mode)
        val routeEntity = routeById(routeId)
            ?: return ActiveTourSnapshot(route = null, lastOptimizeMode = mode)
        val stops = stopsForRoute(routeId)
        val cargo = if (stops.isEmpty()) {
            emptyList()
        } else {
            cargoForStops(stops.map { it.id })
        }
        val cargoByStop = cargo.groupBy { it.stopId }
        return ActiveTourSnapshot(
            route = RouteEntityMapper.toDomainRoute(routeEntity, stops, cargoByStop),
            lastOptimizeMode = mode,
        )
    }

    @Transaction
    suspend fun replaceActiveTour(route: Route?, lastOptimizeMode: OptimizeMode?) {
        deleteAllCargo()
        deleteAllStops()
        deleteAllRoutes()
        if (route == null) {
            upsertMeta(
                ActiveTourMetaEntity(
                    activeRouteId = null,
                    lastOptimizeMode = lastOptimizeMode?.name,
                ),
            )
            return
        }
        upsertRoute(RouteEntityMapper.toRouteEntity(route))
        val stopEntities = RouteEntityMapper.toStopEntities(route.id, route.stops)
        if (stopEntities.isNotEmpty()) {
            upsertStops(stopEntities)
        }
        val cargoEntities = RouteEntityMapper.toCargoEntities(route.stops)
        if (cargoEntities.isNotEmpty()) {
            upsertCargo(cargoEntities)
        }
        upsertMeta(
            ActiveTourMetaEntity(
                activeRouteId = route.id,
                lastOptimizeMode = lastOptimizeMode?.name,
            ),
        )
    }
}

/** Hydrated active tour from Room. */
data class ActiveTourSnapshot(
    val route: Route?,
    val lastOptimizeMode: OptimizeMode?,
)
