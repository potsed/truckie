package com.truckie1.app.trips

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature — ff_route_start_end catalog.
 */
class RouteStartEndCatalogTest {

    @Test
    fun flagKeyDefaultsOff() {
        assertEquals("ff_route_start_end", FlagKeys.ROUTE_START_END)
        assertEquals(FlagKeys.ROUTE_START_END, RouteStartEndCatalog.FLAG)
        val flags = LocalFeatureFlags()
        assertFalse(RouteStartEndCatalog.isEnabled(flags))
        assertFalse(RouteStartEndCatalog.requiresEndpointsOnCreate(flags))
        flags.setEnabled(FlagKeys.ROUTE_START_END, true)
        assertTrue(RouteStartEndCatalog.isEnabled(flags))
        assertTrue(RouteStartEndCatalog.requiresEndpointsOnCreate(flags))
    }
}
