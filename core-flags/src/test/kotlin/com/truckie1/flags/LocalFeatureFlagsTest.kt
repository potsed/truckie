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

    @Test
    fun navChromeFlagKey() {
        assertTrue(FlagKeys.NAV_CHROME == "ff_nav_chrome")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NAV_CHROME))
        flags.setEnabled(FlagKeys.NAV_CHROME, true)
        assertTrue(flags.isEnabled(FlagKeys.NAV_CHROME))
    }

    @Test
    fun mapHomeFlagKey() {
        assertTrue(FlagKeys.MAP_HOME == "ff_map_home")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_HOME))
        flags.setEnabled(FlagKeys.MAP_HOME, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_HOME))
    }

    @Test
    fun addressAutocompleteFlagKey() {
        assertTrue(FlagKeys.ADDRESS_AUTOCOMPLETE == "ff_address_autocomplete")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE))
        flags.setEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE, true)
        assertTrue(flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE))
    }

    @Test
    fun mapLocationPuckFlagKey() {
        assertTrue(FlagKeys.MAP_LOCATION_PUCK == "ff_map_location_puck")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
        flags.setEnabled(FlagKeys.MAP_LOCATION_PUCK, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
    }
}
