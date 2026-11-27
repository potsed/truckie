package com.truckie1.app.ui

import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Legacy alias coverage — prefer [MapStopMarkersCatalogTest].
 * Ensures stop markers use SymbolManager, not deprecated MarkerOptions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapStopAnnotationsTest {

    @Test
    fun usesSymbolManagerNotDeprecatedMarkerApi() {
        assertTrue(MapStopAnnotations.usesSymbolManager)
        assertFalse(MapStopAnnotations.usesDeprecatedMarkerApi)
        assertEquals(MapStopMarkersCatalog.ICON_PICKUP, MapStopAnnotations.ICON_ID)
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

    @Test
    fun aliasPointsAtStopMarkersCatalog() {
        assertEquals(MapStopMarkersCatalog.ICON_PICKUP, MapStopAnnotations.iconIdFor(StopKind.PICKUP_LOAD))
        assertEquals(MapStopMarkersCatalog.ICON_DROPOFF, MapStopAnnotations.iconIdFor(StopKind.DROP_OFF_UNLOAD))
    }
}
