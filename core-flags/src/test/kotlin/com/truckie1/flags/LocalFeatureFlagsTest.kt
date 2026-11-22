package com.truckie1.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFeatureFlagsTest {
    @Test
    fun defaultsOff() {
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VEHICLE_PROFILES))
        assertFalse(flags.isEnabled(FlagKeys.MULTI_STOP))
        assertFalse(flags.isEnabled(FlagKeys.STOP_CARGO))
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
    }

    @Test
    fun canEnableFlag() {
        val flags = LocalFeatureFlags()
        flags.setEnabled(FlagKeys.TOLLS, true)
        assertTrue(flags.isEnabled(FlagKeys.TOLLS))
    }

    @Test
    fun routeOptimizeFlagKey() {
        assertTrue(FlagKeys.ROUTE_OPTIMIZE == "ff_route_optimize")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ROUTE_OPTIMIZE))
        flags.setEnabled(FlagKeys.ROUTE_OPTIMIZE, true)
        assertTrue(flags.isEnabled(FlagKeys.ROUTE_OPTIMIZE))
    }
}
