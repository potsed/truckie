package com.truckie1.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFeatureFlagsTest {
    @Test
    fun defaultsOff() {
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VEHICLE_PROFILES))
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
    }

    @Test
    fun canEnableFlag() {
        val flags = LocalFeatureFlags()
        flags.setEnabled(FlagKeys.TOLLS, true)
        assertTrue(flags.isEnabled(FlagKeys.TOLLS))
    }
}
