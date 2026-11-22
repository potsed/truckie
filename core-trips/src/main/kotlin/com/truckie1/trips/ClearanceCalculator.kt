package com.truckie1.trips

import kotlin.math.max

/**
 * System clearance buffer configured only in Settings.
 * Allowed range: 0.15 m … 1.0 m inclusive.
 */
object ClearanceBuffer {
    const val MIN_M = 0.15
    const val MAX_M = 1.0
    const val DEFAULT_M = 0.15

    fun clamp(bufferM: Double): Double = bufferM.coerceIn(MIN_M, MAX_M)
}

/**
 * Effective clearance height = max(vehicleHeight, tallestCargo) + buffer.
 */
object ClearanceCalculator {
    fun effectiveHeightM(
        vehicleHeightM: Double,
        cargoHeightsM: Collection<Double>,
        bufferM: Double,
    ): Double {
        require(vehicleHeightM > 0) { "vehicleHeightM must be positive" }
        val tallestCargo = cargoHeightsM.maxOrNull() ?: 0.0
        val buffer = ClearanceBuffer.clamp(bufferM)
        return max(vehicleHeightM, tallestCargo) + buffer
    }

    fun effectiveHeightForTripM(
        vehicleHeightM: Double,
        stops: Collection<TripStop>,
        bufferM: Double,
    ): Double {
        val cargoHeights = stops.flatMap { stop -> stop.cargo.map { it.heightM } }
        return effectiveHeightM(vehicleHeightM, cargoHeights, bufferM)
    }
}
