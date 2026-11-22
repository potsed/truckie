package com.truckie1.app.ui

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Gherkin: compose_ui.feature — high-contrast Material3 tokens (PR #6 intent).
 * Pure JVM — no android.graphics (not mocked in local unit tests).
 */
class TruckieColorSchemeTest {

    @Test
    fun primaryLabelContrastsWithNavyFill() {
        val ratio = contrastRatio(TruckiePalette.Navy.toArgb(), TruckiePalette.OnPrimary.toArgb())
        assertTrue("onPrimary vs primary must be WCAG AA (got $ratio)", ratio >= 4.5)
    }

    @Test
    fun bodyTextContrastsWithSkyBackground() {
        val ratio = contrastRatio(TruckiePalette.Navy.toArgb(), TruckiePalette.Sky.toArgb())
        assertTrue("navy on sky must be WCAG AA (got $ratio)", ratio >= 4.5)
    }

    @Test
    fun schemeMapsPrimaryAndOnPrimaryExplicitly() {
        val scheme = TruckieColorScheme.light()
        assertEquals(TruckiePalette.Navy, scheme.primary)
        assertEquals(TruckiePalette.OnPrimary, scheme.onPrimary)
        assertEquals(TruckiePalette.Sky, scheme.background)
        assertEquals(TruckiePalette.Navy, scheme.onBackground)
    }

    private fun contrastRatio(colorA: Int, colorB: Int): Double {
        val l1 = relativeLuminance(colorA)
        val l2 = relativeLuminance(colorB)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
