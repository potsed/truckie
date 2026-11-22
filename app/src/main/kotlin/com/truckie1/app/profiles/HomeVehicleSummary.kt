package com.truckie1.app.profiles

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository

/**
 * Home active-vehicle line, gated by [FlagKeys.VEHICLE_PROFILES].
 */
object HomeVehicleSummary {
    fun text(
        flags: FeatureFlags,
        repo: VehicleProfileRepository,
    ): String? {
        if (!flags.isEnabled(FlagKeys.VEHICLE_PROFILES)) return null
        val active = repo.activeRego?.let { repo.findByRego(it) } ?: return "No active vehicle"
        return format(active)
    }

    fun format(vehicle: Vehicle): String =
        "Active: ${vehicle.rego} · ${vehicle.vehicleType} · ${"%.1f".format(vehicle.heightM)}m"
}
