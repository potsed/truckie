package com.truckie1.app.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OpenFreeMap Liberty only hosts Noto Sans glyphs. MapLibre’s default Open Sans
 * stack 404s and hides SymbolManager icons when textField is set (clearance labels).
 *
 * Gherkin: features/bridge_markers.feature — OpenFreeMap-compatible text font
 */
class MapLibreSymbolTextTest {

    @Test
    fun openFreeMapUsesNotoSansNotMapLibreDefaultStack() {
        assertTrue(MapLibreSymbolText.requiresOpenFreeMapCompatibleTextFont)
        assertEquals("Noto Sans Regular", MapLibreSymbolText.OPENFREEMAP_REGULAR)
        assertArrayEquals(
            arrayOf("Noto Sans Regular"),
            MapLibreSymbolText.openFreeMapTextFont,
        )
        val joined = MapLibreSymbolText.openFreeMapTextFont.joinToString(",")
        assertFalse(
            "Must not request OpenFreeMap-missing default stack: $joined",
            joined.contains("Open Sans", ignoreCase = true),
        )
        assertFalse(
            "Must not request Arial Unicode (not on OpenFreeMap): $joined",
            joined.contains("Arial", ignoreCase = true),
        )
    }

    @Test
    fun textOptionalKeepsIconIfGlyphsFail() {
        assertTrue(
            "textOptional=true keeps W5-8 icons visible if labels fail",
            MapLibreSymbolText.textOptional,
        )
    }

    @Test
    fun bridgeAndStopMarkersShareOpenFreeMapFontContract() {
        assertTrue(BridgeMarkersVisibility.requiresOpenFreeMapCompatibleTextFont)
        assertTrue(MapStopAnnotations.requiresOpenFreeMapCompatibleTextFont)
        assertArrayEquals(
            MapLibreSymbolText.openFreeMapTextFont,
            BridgeMarkersVisibility.symbolTextFont,
        )
        assertArrayEquals(
            MapLibreSymbolText.openFreeMapTextFont,
            MapStopAnnotations.symbolTextFont,
        )
    }
}
