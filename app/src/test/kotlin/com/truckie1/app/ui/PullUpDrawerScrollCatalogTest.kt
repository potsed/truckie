package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/pull_up_drawer_scroll.feature — visible-height sheet + no Jump FAB.
 */
class PullUpDrawerScrollCatalogTest {

    @Test
    fun sheetIsSizedToVisibleHeightWithScrollAndInsets() {
        assertTrue(MapHomeCatalog.sizesSheetToVisibleHeight)
        assertTrue(MapHomeCatalog.stopsListScrollsWithinVisibleSheet)
        assertTrue(MapHomeCatalog.appliesNavigationBarInsetsToSheetContent)
        assertTrue(MapHomeCatalog.appliesImeInsetsToSheetContent)
    }

    @Test
    fun visibleSheetHeightIsContainerMinusTopOffset() {
        assertEquals(500f, MapHomeCatalog.visibleSheetHeightPx(1000f, 500f), 0.1f)
        assertEquals(920f, MapHomeCatalog.visibleSheetHeightPx(1000f, 80f), 0.1f)
        assertEquals(120f, MapHomeCatalog.visibleSheetHeightPx(1000f, 880f), 0.1f)
        // Clamp: offset past bottom → zero height; negative offset → full height.
        assertEquals(0f, MapHomeCatalog.visibleSheetHeightPx(1000f, 1200f), 0.1f)
        assertEquals(1000f, MapHomeCatalog.visibleSheetHeightPx(1000f, -40f), 0.1f)
    }

    @Test
    fun fullAnchorVisibleHeightCoversMostOfPhoneViewport() {
        val container = 780f
        val fullOffset = MapHomeCatalog.sheetAnchorOffsets(container).getValue(RouteSheetValue.Full)
        val visible = MapHomeCatalog.visibleSheetHeightPx(container, fullOffset)
        assertEquals(container * MapHomeCatalog.fullFraction, visible, 0.1f)
        assertTrue(visible > container * 0.85f)
    }

    @Test
    fun jumpToSampleBridgesFabIsRemovedFromMapChrome() {
        assertFalse(MapHomeCatalog.showsJumpToSampleBridgesFab)
        assertFalse(JumpToSampleBridges.showsFloatingMapControl)
    }
}
