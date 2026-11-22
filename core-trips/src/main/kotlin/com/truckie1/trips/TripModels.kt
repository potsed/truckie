package com.truckie1.trips

enum class CargoAction {
    PICK_UP,
    DROP_OFF,
}

/**
 * Cargo dimensions and mass attached to a stop (not the vehicle shell alone).
 */
data class Cargo(
    val description: String,
    val heightM: Double,
    val widthM: Double,
    val lengthM: Double,
    val massT: Double,
    val action: CargoAction,
) {
    init {
        require(heightM > 0) { "cargo heightM must be positive" }
        require(widthM > 0) { "cargo widthM must be positive" }
        require(lengthM > 0) { "cargo lengthM must be positive" }
        require(massT > 0) { "cargo massT must be positive" }
    }
}

data class TripStop(
    val id: String,
    val addressLabel: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cargo: List<Cargo> = emptyList(),
)

data class Trip(
    val id: String,
    val stops: List<TripStop>,
)

enum class OptimizeMode {
    FASTEST,
    CHEAPEST,
}
