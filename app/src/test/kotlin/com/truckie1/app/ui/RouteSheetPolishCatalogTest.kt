package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: route_sheet_polish.feature — Material3 polish tokens for the drag sheet.
 */
class RouteSheetPolishCatalogTest {

    @Test
    fun sheetChromeUsesElevatedSurfaceAndClearHandle() {
        assertTrue(RouteSheetPolishCatalog.topCornerRadiusDp >= 24)
        assertTrue(RouteSheetPolishCatalog.dragHandleWidthDp >= 40)
        assertTrue(RouteSheetPolishCatalog.dragHandleHeightDp >= 4)
        assertTrue(RouteSheetPolishCatalog.dragHandleUsesOnSurfaceVariant)
        assertTrue(RouteSheetPolishCatalog.tonalElevationDp >= 2)
        assertTrue(RouteSheetPolishCatalog.shadowElevationDp >= 6)
        assertTrue(RouteSheetPolishCatalog.preservesDragGestures)
        assertTrue(MapHomeCatalog.usesDragGestures)
        assertTrue(MapHomeCatalog.usesMaterial3DragHandle)
        assertTrue(MapHomeCatalog.usesNestedScrollBridge)
    }

    @Test
    fun peekSummaryUsesReadableTypographyAndPadding() {
        assertTrue(RouteSheetPolishCatalog.summaryUsesTitleSmall)
        assertTrue(RouteSheetPolishCatalog.contentHorizontalPaddingDp >= 16)
        assertTrue(RouteSheetPolishCatalog.peekContentVerticalPaddingDp >= 4)
    }

    @Test
    fun createRouteAndStopsUseIntentionalHierarchy() {
        assertTrue(RouteSheetPolishCatalog.createRouteUsesHeadlineTitle)
        assertTrue(RouteSheetPolishCatalog.dateChoicesUseOutlinedCards)
        assertTrue(RouteSheetPolishCatalog.addFindUsesSearchLeadingIcon)
        assertTrue(RouteSheetPolishCatalog.stopKindsUseFilterChips)
        assertTrue(RouteSheetPolishCatalog.suggestionsUseSurfaceRows)
        assertTrue(RouteSheetPolishCatalog.stopRowsUseTimelineBadge)
        assertTrue(RouteSheetPolishCatalog.sectionSpacingDp >= 16)
    }

    @Test
    fun polishKeepsDomainAndSheetLocalChrome() {
        assertTrue(RouteSheetPolishCatalog.domainUsesRouteAndStops)
        assertEquals(
            listOf("pickup/load", "drop-off/unload"),
            RouteSheetPolishCatalog.stopKindLabels,
        )
        assertTrue(RouteSheetPolishCatalog.hidesGlobalHeaderFooter)
        assertTrue(MapHomeCatalog.hidesGlobalHeaderFooter)
    }
}
