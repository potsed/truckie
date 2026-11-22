package com.truckie1.trips

/**
 * Human-readable stop row text for the trip stops list.
 */
object TripStopSummary {
    fun title(index: Int, stop: TripStop): String {
        val cargo = cargoLine(stop)
        return if (cargo.isEmpty()) {
            "${index + 1} ${stop.addressLabel}"
        } else {
            "${index + 1} ${stop.addressLabel} · $cargo"
        }
    }

    fun cargoLine(stop: TripStop): String {
        if (stop.cargo.isEmpty()) return ""
        val picks = stop.cargo.count { it.action == CargoAction.PICK_UP }
        val drops = stop.cargo.count { it.action == CargoAction.DROP_OFF }
        val parts = mutableListOf<String>()
        if (picks > 0) parts += "pick $picks"
        if (drops > 0) parts += "drop $drops"
        return parts.joinToString(" · ")
    }

    fun tripOverview(stops: List<TripStop>, optimizeMode: OptimizeMode? = null): String {
        if (stops.isEmpty()) return "No stops yet"
        val labels = stops.map { it.addressLabel }
        val path = when {
            labels.size <= 3 -> labels.joinToString(" → ")
            else -> "${labels.first()} → … → ${labels.last()}"
        }
        val modeLabel = when (optimizeMode) {
            OptimizeMode.FASTEST -> " · Fastest"
            OptimizeMode.CHEAPEST -> " · Cheapest"
            null -> ""
        }
        return "Trip: ${stops.size} stops$modeLabel · $path"
    }
}
