package com.truckie1.trips

/**
 * Domain for map-first home (Phase A): a [Route] has ordered [RouteStop]s.
 * Stop kind is pickup/load or drop-off/unload (not legacy “trip” wording in new UI).
 */
enum class StopKind {
    PICKUP_LOAD,
    DROP_OFF_UNLOAD,
}

enum class RouteStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
}

enum class RouteDateChoice {
    TODAY,
    TOMORROW,
    CUSTOM,
}

data class RouteStop(
    val id: String,
    val addressLabel: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val kind: StopKind,
    val order: Int = 0,
)

data class Route(
    val id: String,
    val name: String? = null,
    val dateEpochDay: Long,
    val status: RouteStatus = RouteStatus.DRAFT,
    val stops: List<RouteStop> = emptyList(),
)

/** Peek summary strip text for the route drag sheet. */
object RouteSummary {
    fun strip(route: Route): String {
        val total = route.stops.size
        val pickups = route.stops.count { it.kind == StopKind.PICKUP_LOAD }
        val dropoffs = route.stops.count { it.kind == StopKind.DROP_OFF_UNLOAD }
        val label = route.name?.takeIf { it.isNotBlank() } ?: "Untitled route"
        return "$label · $total stops · $pickups pickup/load · $dropoffs drop-off/unload"
    }

    fun emptyStrip(): String = "No active route · 0 stops"
}
