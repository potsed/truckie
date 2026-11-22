package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/demo_mode.feature — ff_demo_mode + Settings toggle.
 */
class DemoModeCatalogTest {

    @Test
    fun demoModeFlagKeyIsFfDemoMode() {
        assertEquals("ff_demo_mode", FlagKeys.DEMO_MODE)
        assertEquals("ff_demo_mode", DemoModeCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(DemoModeCatalog.isEnabled(flags))
        DemoModeCatalog.setEnabled(flags, enabled = true)
        assertTrue(DemoModeCatalog.isEnabled(flags))
        DemoModeCatalog.setEnabled(flags, enabled = false)
        assertFalse(DemoModeCatalog.isEnabled(flags))
    }

    @Test
    fun settingsShowsDemoModeSwitch() {
        assertTrue(DemoModeCatalog.showsInSettings)
        assertEquals("Demo mode", DemoModeCatalog.SETTINGS_LABEL)
        assertEquals("settings_demo_mode_switch", DemoModeCatalog.SETTINGS_TEST_TAG)
    }
}
