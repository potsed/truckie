package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizeForVehicleGateTest {
    @Test
    fun routeOptimizeFlagKey() {
        assertEquals("ff_route_optimize", FlagKeys.ROUTE_OPTIMIZE)
    }

    @Test
    fun optimizeActionsVisibleOnlyWhenFlagEnabled() {
        val flags = LocalFeatureFlags()
        assertFalse(OptimizeForVehicleGate.isAvailable(flags))
        flags.setEnabled(FlagKeys.ROUTE_OPTIMIZE, true)
        assertTrue(OptimizeForVehicleGate.isAvailable(flags))
    }
}
