package com.truckie1.app.profiles

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted [com.truckie1.trips.Route] header (ATNA-89 / P1-4).
 * Endpoints are nullable columns (draft routes may lack start/end).
 */
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val dateEpochDay: Long,
    val status: String,
    val startAddress: String?,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endAddress: String?,
    val endLatitude: Double?,
    val endLongitude: Double?,
)

/**
 * Persisted [com.truckie1.trips.RouteStop] row ordered within a route.
 */
@Entity(
    tableName = "route_stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routeId"])],
)
data class RouteStopEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val addressLabel: String,
    val latitude: Double?,
    val longitude: Double?,
    val kind: String,
    val stopOrder: Int,
)

/**
 * Persisted [com.truckie1.trips.Cargo] attached to a stop.
 */
@Entity(
    tableName = "route_cargo",
    foreignKeys = [
        ForeignKey(
            entity = RouteStopEntity::class,
            parentColumns = ["id"],
            childColumns = ["stopId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["stopId"])],
)
data class RouteCargoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stopId: String,
    val description: String,
    val heightM: Double,
    val widthM: Double,
    val lengthM: Double,
    val massT: Double,
    val action: String,
    val cargoOrder: Int,
)

/**
 * Singleton row: which route is active + last Optimise mode.
 */
@Entity(tableName = "active_tour_meta")
data class ActiveTourMetaEntity(
    @PrimaryKey val id: Int = 1,
    val activeRouteId: String?,
    val lastOptimizeMode: String?,
)
