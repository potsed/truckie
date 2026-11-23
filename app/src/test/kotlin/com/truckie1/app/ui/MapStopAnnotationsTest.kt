package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ensures stop markers use SymbolManager, not deprecated MarkerOptions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapStopAnnotationsTest {

    @Test
    fun usesSymbolManagerNotDeprecatedMarkerApi() {
        assertTrue(MapStopAnnotations.usesSymbolManager)
        assertFalse(MapStopAnnotations.usesDeprecatedMarkerApi)
        assertEquals("truckie_stop_marker", MapStopAnnotations.ICON_ID)
        assertTrue(MapStopAnnotations.requiresOpenFreeMapCompatibleTextFont)
        assertEquals("Noto Sans Regular", MapStopAnnotations.symbolTextFont.single())
    }

    @Test
    fun createsNonEmptyStopMarkerBitmap() {
        val bitmap = MapStopAnnotations.createStopMarkerBitmap(48)
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(48, bitmap.height)
        assertFalse(bitmap.isRecycled)
    }
}
