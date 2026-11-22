package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — sheet anchors + Compose catalog for map home.
 */
class MapHomeCatalogTest {

    @Test
    fun mapHomeFlagKeyIsFfMapHome() {
        assertEquals("ff_map_home", FlagKeys.MAP_HOME)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_HOME))
        flags.setEnabled(FlagKeys.MAP_HOME, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_HOME))
    }

    @Test
    fun sheetExposesPeekHalfAndFullAnchors() {
        assertEquals(
            listOf(RouteSheetValue.Peek, RouteSheetValue.Half, RouteSheetValue.Full),
            MapHomeCatalog.sheetAnchors,
        )
        assertEquals(0.12f, MapHomeCatalog.peekFraction, 0.001f)
        assertEquals(0.5f, MapHomeCatalog.halfFraction, 0.001f)
        assertEquals(0.92f, MapHomeCatalog.fullFraction, 0.001f)
    }

    @Test
    fun peekShowsSummaryWhileHalfAndFullRevealRouteContent() {
        assertTrue(MapHomeCatalog.showsSummaryStrip(RouteSheetValue.Peek))
        assertTrue(MapHomeCatalog.showsSummaryStrip(RouteSheetValue.Half))
        assertTrue(MapHomeCatalog.showsSummaryStrip(RouteSheetValue.Full))
        assertFalse(MapHomeCatalog.showsCreateRouteForm(RouteSheetValue.Peek))
        assertTrue(MapHomeCatalog.showsCreateRouteForm(RouteSheetValue.Half))
        assertTrue(MapHomeCatalog.showsCreateRouteForm(RouteSheetValue.Full))
        assertFalse(MapHomeCatalog.showsStopList(RouteSheetValue.Peek))
        assertTrue(MapHomeCatalog.showsStopList(RouteSheetValue.Half))
        assertTrue(MapHomeCatalog.showsStopList(RouteSheetValue.Full))
    }

    @Test
    fun mapHomeIsComposeOnlyOnMainActivityNavHost() {
        assertTrue(MapHomeCatalog.composeOnly)
        assertFalse(MapHomeCatalog.allowsXmlLayouts)
        assertEquals("MainActivity", MapHomeCatalog.hostActivitySimpleName)
        assertTrue(MapHomeCatalog.hostedInNavHost)
        assertTrue(MapHomeCatalog.keepsHighContrastChrome)
        assertEquals("MapLibre", MapHomeCatalog.mapEngineName)
    }

    @Test
    fun stopKindsArePickupLoadAndDropOffUnload() {
        assertEquals(
            listOf("pickup/load", "drop-off/unload"),
            MapHomeCatalog.stopKindLabels,
        )
    }

    @Test
    fun usesMapHomeWhenFlagEnabled() {
        val off = LocalFeatureFlags()
        assertFalse(MapHomeCatalog.usesMapHome(off))
        val on = LocalFeatureFlags(mapOf(FlagKeys.MAP_HOME to true))
        assertTrue(MapHomeCatalog.usesMapHome(on))
    }
}
