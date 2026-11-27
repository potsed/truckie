package com.truckie1.trips

/**
 * Trip stop list store (reorder / add / delete).
 * Real and demo modes share this contract — only the backing dataset differs.
 */
interface TripRepository {
    /** Last successful optimize mode (Fastest / Cheapest), if any. */
    var lastOptimizeMode: OptimizeMode?

    fun stops(): List<TripStop>
    fun addStop(stop: TripStop)
    fun removeStop(id: String)
    fun moveStop(fromIndex: Int, toIndex: Int)
    fun replaceStops(newStops: List<TripStop>)
    fun updateStop(stop: TripStop)
}

/**
 * In-memory editable trip stop list (reorder / add / delete).
 */
class TripSession(
    initialStops: List<TripStop> = emptyList(),
) : TripRepository {
    private val stops = initialStops.toMutableList()

    override var lastOptimizeMode: OptimizeMode? = null

    override fun stops(): List<TripStop> = stops.toList()

    override fun addStop(stop: TripStop) {
        require(stops.none { it.id == stop.id }) { "Duplicate stop id: ${stop.id}" }
        stops.add(stop)
    }

    override fun removeStop(id: String) {
        stops.removeAll { it.id == id }
    }

    override fun moveStop(fromIndex: Int, toIndex: Int) {
        require(fromIndex in stops.indices) { "fromIndex out of range" }
        require(toIndex in stops.indices) { "toIndex out of range" }
        if (fromIndex == toIndex) return
        val item = stops.removeAt(fromIndex)
        stops.add(toIndex, item)
    }

    override fun replaceStops(newStops: List<TripStop>) {
        stops.clear()
        stops.addAll(newStops)
    }

    override fun updateStop(stop: TripStop) {
        val index = stops.indexOfFirst { it.id == stop.id }
        require(index >= 0) { "Unknown stop id: ${stop.id}" }
        stops[index] = stop
    }
}
