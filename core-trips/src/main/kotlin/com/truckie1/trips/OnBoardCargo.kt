package com.truckie1.trips

/**
 * Simulates cargo remaining on the vehicle after processing stops in order.
 * Pick-ups add items; drop-offs remove the first on-board item with the same
 * description (case-insensitive, trimmed).
 */
object OnBoardCargo {
    fun afterStops(
        stops: List<TripStop>,
        throughIndexInclusive: Int,
    ): List<Cargo> {
        if (stops.isEmpty() || throughIndexInclusive < 0) return emptyList()
        val end = throughIndexInclusive.coerceAtMost(stops.lastIndex)
        val onBoard = mutableListOf<Cargo>()
        for (i in 0..end) {
            applyStopCargo(onBoard, stops[i].cargo)
        }
        return onBoard.toList()
    }

    fun afterAll(stops: List<TripStop>): List<Cargo> =
        if (stops.isEmpty()) emptyList() else afterStops(stops, stops.lastIndex)

    private fun applyStopCargo(onBoard: MutableList<Cargo>, cargo: List<Cargo>) {
        for (item in cargo) {
            when (item.action) {
                CargoAction.PICK_UP -> onBoard.add(item)
                CargoAction.DROP_OFF -> {
                    val key = normalize(item.description)
                    val index = onBoard.indexOfFirst { normalize(it.description) == key }
                    if (index >= 0) onBoard.removeAt(index)
                }
            }
        }
    }

    private fun normalize(description: String): String = description.trim().lowercase()
}
