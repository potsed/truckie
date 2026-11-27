package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/navigate_multi_stop_path.feature — Navigate multi-stop catalog.
 */
class NavigateMultiStopCatalogTest {

    @Test
    fun flagKeyAndDefaultsOnConstant() {
        assertEquals("ff_navigate_multi_stop", FlagKeys.NAVIGATE_MULTI_STOP)
        assertEquals(FlagKeys.NAVIGATE_MULTI_STOP, NavigateMultiStopCatalog.FLAG_KEY)
        assertTrue(NavigateMultiStopCatalog.DEFAULT_ENABLED)
        assertTrue(NavigateMultiStopCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(NavigateMultiStopCatalog.usesActiveTourPath)
        assertTrue(NavigateMultiStopCatalog.usesMultiStopTourComposer)
        assertFalse(NavigateMultiStopCatalog.allowsStartToEndOnlyPath)
        assertTrue(NavigateMultiStopCatalog.sharesGeometryWithMapPolyline)
    }

    @Test
    fun setEnabledTogglesFlag() {
        val flags = LocalFeatureFlags()
        assertFalse(NavigateMultiStopCatalog.isEnabled(flags))
        NavigateMultiStopCatalog.setEnabled(flags, true)
        assertTrue(NavigateMultiStopCatalog.isEnabled(flags))
        NavigateMultiStopCatalog.setEnabled(flags, false)
        assertFalse(NavigateMultiStopCatalog.isEnabled(flags))
    }
}
