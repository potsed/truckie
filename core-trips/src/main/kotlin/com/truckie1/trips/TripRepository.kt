package com.truckie1.trips

/**
 * Trip stop list store (reorder / add / delete) — projection over the active
 * [Route] when using [ActiveTourSession] (ATNA-86).
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
