package com.truckie1.app.auto

import com.truckie1.app.AndroidAutoGate
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/android_auto.feature — ff_android_auto behaviour contracts (ATNA-95).
 */
class AndroidAutoCatalogTest {

    @Test
    fun flagKeyDefaultsOffIncludingDebug() {
        assertEquals("ff_android_auto", FlagKeys.ANDROID_AUTO)
        assertEquals(FlagKeys.ANDROID_AUTO, AndroidAutoCatalog.FLAG_KEY)
        assertFalse(AndroidAutoCatalog.DEFAULT_ENABLED)
        assertFalse(AndroidAutoCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
        assertFalse(AndroidAutoCatalog.isEnabled(flags, sdkInt = 30))
    }

    @Test
    fun gateRequiresFlagAndApi23() {
        val on = LocalFeatureFlags(mapOf(FlagKeys.ANDROID_AUTO to true))
        assertFalse(AndroidAutoCatalog.isEnabled(on, sdkInt = 22))
        assertTrue(AndroidAutoCatalog.isEnabled(on, sdkInt = 23))
        assertEquals(23, AndroidAutoCatalog.MIN_API)
        assertEquals(AndroidAutoGate.MIN_API, AndroidAutoCatalog.MIN_API)

        val off = LocalFeatureFlags()
        assertFalse(AndroidAutoCatalog.isEnabled(off, sdkInt = 30))
    }

    @Test
    fun scaffoldUntilNavigationTemplateShips() {
        assertTrue(AndroidAutoCatalog.usesScaffoldMessageTemplateToday)
        assertFalse(AndroidAutoCatalog.shipsNavigationTemplateYet)
        assertTrue(AndroidAutoCatalog.requiresNavigationTemplateBeforeProduction)
        assertTrue(AndroidAutoCatalog.nextDestinationMustShowClearlyOnAuto)
        assertEquals("ATNA-10", AndroidAutoCatalog.trackedBy)
    }

    @Test
    fun phoneScreenOnAndAutoBlankContracted() {
        assertTrue(AndroidAutoCatalog.keepPhoneScreenActiveWhileNavigating)
        assertTrue(AndroidAutoCatalog.phoneMayBlankWhenAutoConnected)
        assertTrue(AndroidAutoCatalog.releaseHostValidationIsStrict)
    }
}
