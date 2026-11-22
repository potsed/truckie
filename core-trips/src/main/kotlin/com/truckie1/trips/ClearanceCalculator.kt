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

    /**
     * Legacy helper: max height across all cargo listed on stops (not on-board state).
     * Prefer [effectiveHeightAfterStopM] / [maxEffectiveHeightAlongTripM] for routing.
     */
    fun effectiveHeightForTripM(
        vehicleHeightM: Double,
        stops: Collection<TripStop>,
        bufferM: Double,
    ): Double {
        val cargoHeights = stops.flatMap { stop -> stop.cargo.map { it.heightM } }
        return effectiveHeightM(vehicleHeightM, cargoHeights, bufferM)
    }

    fun effectiveHeightWithOnBoardM(
        vehicleHeightM: Double,
        onBoardCargo: Collection<Cargo>,
        bufferM: Double,
    ): Double = effectiveHeightM(
        vehicleHeightM = vehicleHeightM,
        cargoHeightsM = onBoardCargo.map { it.heightM },
        bufferM = bufferM,
    )

    /** Effective clearance after processing stops[0..throughIndexInclusive]. */
    fun effectiveHeightAfterStopM(
        vehicleHeightM: Double,
        stops: List<TripStop>,
        throughIndexInclusive: Int,
        bufferM: Double,
    ): Double = effectiveHeightWithOnBoardM(
        vehicleHeightM = vehicleHeightM,
        onBoardCargo = OnBoardCargo.afterStops(stops, throughIndexInclusive),
        bufferM = bufferM,
    )

    /**
     * Worst-case effective clearance along the trip: max after each stop
     * (including empty vehicle before any stop when the list is empty).
     */
    fun maxEffectiveHeightAlongTripM(
        vehicleHeightM: Double,
        stops: List<TripStop>,
        bufferM: Double,
    ): Double {
        if (stops.isEmpty()) {
            return effectiveHeightM(vehicleHeightM, emptyList(), bufferM)
        }
        var peak = effectiveHeightM(vehicleHeightM, emptyList(), bufferM)
        for (i in stops.indices) {
            val atStop = effectiveHeightAfterStopM(vehicleHeightM, stops, i, bufferM)
            if (atStop > peak) peak = atStop
        }
        return peak
    }
}
