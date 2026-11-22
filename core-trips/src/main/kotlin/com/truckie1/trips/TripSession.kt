package com.truckie1.trips

/**
 * In-memory editable trip stop list (reorder / add / delete).
 */
class TripSession(
    initialStops: List<TripStop> = emptyList(),
) {
    private val stops = initialStops.toMutableList()

    fun stops(): List<TripStop> = stops.toList()

    fun addStop(stop: TripStop) {
        require(stops.none { it.id == stop.id }) { "Duplicate stop id: ${stop.id}" }
        stops.add(stop)
    }

    fun removeStop(id: String) {
        stops.removeAll { it.id == id }
    }

    fun moveStop(fromIndex: Int, toIndex: Int) {
        require(fromIndex in stops.indices) { "fromIndex out of range" }
        require(toIndex in stops.indices) { "toIndex out of range" }
        if (fromIndex == toIndex) return
        val item = stops.removeAt(fromIndex)
        stops.add(toIndex, item)
    }

    fun replaceStops(newStops: List<TripStop>) {
        stops.clear()
        stops.addAll(newStops)
    }

    fun updateStop(stop: TripStop) {
        val index = stops.indexOfFirst { it.id == stop.id }
        require(index >= 0) { "Unknown stop id: ${stop.id}" }
        stops[index] = stop
    }
}
