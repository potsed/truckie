package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.routing.LatLngPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_path_from_location.feature — flag + approach policy.
 */
class RoutePathFromLocationCatalogTest {

    @Test
    fun flagKeyAndDefaults() {
        assertEquals("ff_route_path_from_location", FlagKeys.ROUTE_PATH_FROM_LOCATION)
        assertEquals(FlagKeys.ROUTE_PATH_FROM_LOCATION, RoutePathFromLocationCatalog.FLAG_KEY)
        assertTrue(RoutePathFromLocationCatalog.DEFAULT_ENABLED)
        assertTrue(RoutePathFromLocationCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(RoutePathFromLocationCatalog.usesActiveTourPathApproach)
        assertTrue(RoutePathFromLocationCatalog.approachIsPathRouterLeg)
        assertFalse(RoutePathFromLocationCatalog.allowsDiyStraightLineApproach)
        assertTrue(RoutePathFromLocationCatalog.fallbackWithoutFixIsStartThroughEnd)
    }

    @Test
    fun approachFixRespectsFlag() {
        val fix = LatLngPoint(-27.4, 153.08)
        val off = LocalFeatureFlags()
        assertNull(RoutePathFromLocationCatalog.approachFix(off, fix))
        val on = LocalFeatureFlags(mapOf(FlagKeys.ROUTE_PATH_FROM_LOCATION to true))
        assertEquals(fix, RoutePathFromLocationCatalog.approachFix(on, fix))
        assertNull(RoutePathFromLocationCatalog.approachFix(on, null))
    }
}
