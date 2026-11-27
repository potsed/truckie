package com.truckie1.trips

/**
 * Domain for map-first home (Phase A / ATNA-86): a [Route] has ordered [RouteStop]s.
 * Stop kind is pickup/load or drop-off/unload. Cargo lives on [RouteStop];
 * [TripStop] is a projection for Optimise / clearance / mass.
 *
 * A route also has verified [start] and [end] endpoints (same place = round trip).
 * Single SoT: [ActiveTourSession] — no parallel TripSession list.
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

/**
 * Verified place used as a route start or end (formatted address + lat/lng).
 */
data class RouteEndpoint(
    val addressLabel: String,
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(addressLabel.isNotBlank()) { "addressLabel must not be blank" }
    }

    /** Same place when coordinates match (labels may differ slightly). */
    fun samePlaceAs(other: RouteEndpoint): Boolean =
        latitude == other.latitude && longitude == other.longitude
}

data class RouteStop(
    val id: String,
    val addressLabel: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val kind: StopKind,
    val order: Int = 0,
    /** On-stop cargo for clearance / mass / Optimise (projected to [TripStop]). */
    val cargo: List<Cargo> = emptyList(),
)

data class Route(
    val id: String,
    val name: String? = null,
    val dateEpochDay: Long,
    val status: RouteStatus = RouteStatus.DRAFT,
    val start: RouteEndpoint? = null,
    val end: RouteEndpoint? = null,
    val stops: List<RouteStop> = emptyList(),
) {
    val hasEndpoints: Boolean
        get() = start != null && end != null

    /** Same start and end place is a valid round trip. */
    val isRoundTrip: Boolean
        get() {
            val s = start ?: return false
            val e = end ?: return false
            return s.samePlaceAs(e)
        }
}

/** Peek summary strip text for the route drag sheet. */
object RouteSummary {
    fun strip(route: Route): String {
        val total = route.stops.size
        val pickups = route.stops.count { it.kind == StopKind.PICKUP_LOAD }
        val dropoffs = route.stops.count { it.kind == StopKind.DROP_OFF_UNLOAD }
        val label = route.name?.takeIf { it.isNotBlank() } ?: "Untitled route"
        val endpoints = when {
            route.isRoundTrip -> {
                val place = route.start!!.addressLabel
                " · round trip · $place"
            }
            route.hasEndpoints -> {
                " · ${route.start!!.addressLabel} → ${route.end!!.addressLabel}"
            }
            else -> ""
        }
        return "$label$endpoints · $total stops · $pickups pickup/load · $dropoffs drop-off/unload"
    }

    fun emptyStrip(): String = "No active route · 0 stops"
}
