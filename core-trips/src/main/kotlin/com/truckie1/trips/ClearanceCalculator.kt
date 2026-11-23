package com.truckie1.trips

import kotlin.math.max

/**
 * System clearance buffer configured only in Settings.
 * Allowed range: 0.15 m … 1.0 m inclusive.
 *
 * The buffer exists in part because digital pack data can lag roadside reality
 * (e.g. resurfacing) — physical signs always override packs — not only for
 * vehicle/cargo variance.
 */
object ClearanceBuffer {
    const val MIN_M = 0.15
    const val MAX_M = 1.0
    const val DEFAULT_M = 0.15

    /** Rationale surfaced in docs / Settings copy. */
    const val RATIONALE: String =
        "Buffer covers vehicle/cargo variance and digital pack lag versus " +
            "physical roadside signs (packs can be inaccurate or stale)."

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
