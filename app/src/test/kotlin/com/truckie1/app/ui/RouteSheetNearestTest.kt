package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Gherkin: map_first_home.feature — sheet snap to peek / half / full. */
class RouteSheetNearestTest {

    @Test
    fun snapsToNearestAnchor() {
        assertEquals(RouteSheetValue.Peek, nearestSheetValue(0.10f))
        assertEquals(RouteSheetValue.Peek, nearestSheetValue(0.20f))
        assertEquals(RouteSheetValue.Half, nearestSheetValue(0.45f))
        assertEquals(RouteSheetValue.Half, nearestSheetValue(0.55f))
        assertEquals(RouteSheetValue.Full, nearestSheetValue(0.85f))
        assertEquals(RouteSheetValue.Full, nearestSheetValue(0.95f))
    }
}
