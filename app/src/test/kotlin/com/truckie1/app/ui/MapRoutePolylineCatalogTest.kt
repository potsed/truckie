package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_route_polyline.feature — ff_map_route_polyline catalog.
 */
class MapRoutePolylineCatalogTest {

    @Test
    fun routePolylineFlagKeyIsFfMapRoutePolyline() {
        assertEquals("ff_map_route_polyline", FlagKeys.MAP_ROUTE_POLYLINE)
        assertEquals(FlagKeys.MAP_ROUTE_POLYLINE, MapRoutePolylineCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE))
        flags.setEnabled(FlagKeys.MAP_ROUTE_POLYLINE, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE))
    }

    @Test
    fun gatedByFlagAndMapHome() {
        val off = LocalFeatureFlags()
        assertFalse(MapRoutePolylineCatalog.isEnabled(off))
        assertFalse(MapRoutePolylineCatalog.shouldShow(off, mapHomeEnabled = true))
        assertFalse(
            MapRoutePolylineCatalog.shouldShow(
                LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true)),
                mapHomeEnabled = false,
            ),
        )
        val on = LocalFeatureFlags(mapOf(FlagKeys.MAP_ROUTE_POLYLINE to true))
        assertTrue(MapRoutePolylineCatalog.isEnabled(on))
        assertTrue(MapRoutePolylineCatalog.shouldShow(on, mapHomeEnabled = true))
    }

    @Test
    fun usesSharedStyleLineLayerPathNotParallelStack() {
        assertTrue(MapRoutePolylineCatalog.usesStyleLineLayer)
        assertFalse(MapRoutePolylineCatalog.usesLineManager)
        assertTrue(MapRoutePolylineCatalog.usesSharedStyleOverlayBindPath)
        assertTrue(MapRoutePolylineCatalog.sharesStyleLifecycleWithNavigate)
        assertTrue(MapLineBind.sharesPathWithNavigatePolyline)
        assertEquals(
            MapRoutePathStyle.LINE_COLOR_HEX,
            MapRoutePolylineCatalog.LINE_COLOR_HEX,
        )
        assertEquals(
            MapRoutePathStyle.LINE_WIDTH,
            MapRoutePolylineCatalog.LINE_WIDTH,
        )
    }

    @Test
    fun doesNotImplementSatellitePeek() {
        assertFalse(MapRoutePolylineCatalog.implementsSatellitePeek)
    }

    @Test
    fun geometryPolicyUsesPathRouterNotDiyStraightLine() {
        assertTrue(MapRoutePolylineCatalog.usesPathRouterGeometry)
        assertTrue(MapRoutePolylineCatalog.prefersOptimisedTourGeometryWhenAvailable)
        assertFalse(MapRoutePolylineCatalog.allowsDiyStraightLineBetweenStops)
        assertTrue(MapRoutePolylineCatalog.stubGeometryOnlyViaActivePathRouterFactory)
    }

    @Test
    fun constraintPolicyMatchesOptimizeAndNavigate() {
        assertTrue(MapRoutePolylineCatalog.hardBlocksUnderPassageLowBridges)
        assertTrue(MapRoutePolylineCatalog.hardBlocksWeightLimits)
        assertTrue(MapRoutePolylineCatalog.softResidentialPreferenceOnly)
        assertTrue(MapRoutePolylineCatalog.overDeckPassageOk)
        assertTrue(MapRoutePolylineCatalog.appliesTollPrefsWhenFlagged)
        assertTrue(MapRoutePolylineCatalog.usesSamePathRouterAsOptimizeAndNavigate)
        assertTrue(MapRoutePolylineCatalog.usesBridgePassagePolicyAndPackBridges)
    }

    @Test
    fun tourGoalsAreCheckboxesBothOnByDefaultAnd() {
        val goals = MapRoutePolylineCatalog.defaultTourGoals
        assertTrue(goals.preferFastest)
        assertTrue(goals.preferCostEffective)
        assertEquals(
            com.truckie1.trips.TourOptimizeGoals.DEFAULT,
            goals,
        )
    }
}
