package com.truckie1.app

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoGateTest {
    @Test
    fun requiresApi23AndFlag() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.ANDROID_AUTO to true))
        assertFalse(AndroidAutoGate.isEnabled(flags, sdkInt = 22))
        assertTrue(AndroidAutoGate.isEnabled(flags, sdkInt = 23))
        assertFalse(AndroidAutoGate.isEnabled(LocalFeatureFlags(), sdkInt = 30))
    }
}
