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
    /** @deprecated Product goal is cost-effective; use [TourOptimizeGoals]. */
    CHEAPEST,
}

/**
 * Fastest and Cost Effective are **checkboxes** (not radio), both **on by
 * default** — AND preference: prefer tours that are fast and cost-effective
 * together. Map route polyline and Optimize share this model.
 */
data class TourOptimizeGoals(
    val preferFastest: Boolean = true,
    val preferCostEffective: Boolean = true,
) {
    companion object {
        /** Both checkboxes selected — product default. */
        val DEFAULT: TourOptimizeGoals = TourOptimizeGoals(
            preferFastest = true,
            preferCostEffective = true,
        )

        fun fromLegacyMode(mode: OptimizeMode): TourOptimizeGoals = when (mode) {
            OptimizeMode.FASTEST -> TourOptimizeGoals(
                preferFastest = true,
                preferCostEffective = false,
            )
            OptimizeMode.CHEAPEST -> TourOptimizeGoals(
                preferFastest = false,
                preferCostEffective = true,
            )
        }
    }

    /** Legacy single-axis view when exactly one checkbox is on. */
    fun asLegacyMode(): OptimizeMode? = when {
        preferFastest && !preferCostEffective -> OptimizeMode.FASTEST
        !preferFastest && preferCostEffective -> OptimizeMode.CHEAPEST
        else -> null
    }
}
