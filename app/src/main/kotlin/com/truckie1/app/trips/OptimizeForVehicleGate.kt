package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Gates the "Optimize for this vehicle" entry on trip stops.
 */
object OptimizeForVehicleGate {
    fun isAvailable(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ROUTE_OPTIMIZE)
}
