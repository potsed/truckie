package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.TripRepository
import com.truckie1.trips.TripStopSummary

/**
 * Home trip overview and clearance lines, gated by multi-stop / cargo flags.
 */
object HomeTripSummary {
    fun text(flags: FeatureFlags, session: TripRepository): String? {
        if (!flags.isEnabled(FlagKeys.MULTI_STOP)) return null
        return TripStopSummary.tripOverview(
            stops = session.stops(),
            optimizeMode = session.lastOptimizeMode,
        )
    }

    fun clearanceLine(
        flags: FeatureFlags,
        session: TripRepository,
        vehicleHeightM: Double?,
        bufferM: Double,
    ): String? {
        if (vehicleHeightM == null) return null
        val stops = session.stops()
        val effective = if (
            flags.isEnabled(FlagKeys.MULTI_STOP) &&
            flags.isEnabled(FlagKeys.STOP_CARGO)
        ) {
            ClearanceCalculator.maxEffectiveHeightAlongTripM(
                vehicleHeightM = vehicleHeightM,
                stops = stops,
                bufferM = bufferM,
            )
        } else {
            ClearanceCalculator.effectiveHeightM(
                vehicleHeightM = vehicleHeightM,
                cargoHeightsM = emptyList(),
                bufferM = bufferM,
            )
        }
        val withLoad = flags.isEnabled(FlagKeys.STOP_CARGO) &&
            flags.isEnabled(FlagKeys.MULTI_STOP) &&
            stops.any { it.cargo.isNotEmpty() }
        val label = if (withLoad) {
            "Eff. clearance (w/ load + buffer)"
        } else {
            "Eff. clearance (empty + buffer)"
        }
        return "$label: ${"%.2f".format(effective)} m"
    }
}
