package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.ClearanceBuffer

/**
 * Home-map bridge conflict filter by active vehicle height (ATNA-47).
 *
 * Gherkin: features/vehicle_height_bridge_filter.feature
 *
 * When an active vehicle has a usable height, W5-8 symbols show only structures
 * whose published clearance is **below** `vehicleHeight + system buffer`
 * (conflicts). Keeps [LowBridgeW58] low-only rules; never invents heights for
 * unknown / not_applicable / missing clearance.
 *
 * **Fallback** when the flag is on but there is no active vehicle / no usable
 * height: return the input list unchanged (`[NO_HEIGHT_FALLBACK]` = show all
 * low-pack markers) so Demo Jump and dogfood still work.
 *
 * Buffer: existing [ClearanceBuffer] (Settings, default [ClearanceBuffer.DEFAULT_M]).
 */
object VehicleHeightBridgeFilter {
    const val FLAG_KEY: String = FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER

    /** Same default as [ClearanceBuffer.DEFAULT_M] — Settings system buffer. */
    const val DEFAULT_BUFFER_M: Double = ClearanceBuffer.DEFAULT_M

    /**
     * Documented fallback when filter flag is on but height is missing/unusable.
     * `show_all_low` = keep W5-8 low-pack markers visible (prefer over hide).
     */
    const val NO_HEIGHT_FALLBACK: String = "show_all_low"

    const val BUFFER_RATIONALE: String =
        "Uses Settings ClearanceBuffer (default ${ClearanceBuffer.DEFAULT_M} m, " +
            "clamped ${ClearanceBuffer.MIN_M}–${ClearanceBuffer.MAX_M} m). " +
            ClearanceBuffer.RATIONALE

    const val FALLBACK_RATIONALE: String =
        "When no active vehicle or no usable height, show all low-pack W5-8 " +
            "markers so Demo Jump and map dogfood still work."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER)

    /** Usable height is a positive vehicle height in metres. */
    fun hasUsableHeight(vehicleHeightM: Double?): Boolean =
        vehicleHeightM != null && vehicleHeightM > 0.0

    fun conflictThresholdM(vehicleHeightM: Double, bufferM: Double): Double =
        vehicleHeightM + ClearanceBuffer.clamp(bufferM)

    /**
     * True when the bridge has a published clearance strictly below
     * `vehicleHeight + buffer`. Missing / unknown heights never invent a value
     * and never count as a conflict.
     */
    fun conflictsWithVehicle(
        bridge: BridgePoint,
        vehicleHeightM: Double,
        bufferM: Double,
    ): Boolean {
        val clearance = bridge.clearanceHeightM ?: return false
        return clearance < conflictThresholdM(vehicleHeightM, bufferM)
    }

    /**
     * When [ff_vehicle_height_bridge_filter][FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER]
     * is on and [vehicleHeightM] is usable, returns only conflict bridges.
     * Otherwise returns [bridges] unchanged (flag off, or no-height fallback).
     */
    fun bridgesAffectingVehicle(
        bridges: List<BridgePoint>,
        flags: FeatureFlags,
        vehicleHeightM: Double?,
        bufferM: Double = DEFAULT_BUFFER_M,
    ): List<BridgePoint> {
        if (!isEnabled(flags)) return bridges
        if (!hasUsableHeight(vehicleHeightM)) return bridges
        val height = vehicleHeightM!!
        return bridges.filter { conflictsWithVehicle(it, height, bufferM) }
    }
}
