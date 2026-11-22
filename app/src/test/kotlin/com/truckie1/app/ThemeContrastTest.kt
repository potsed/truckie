package com.truckie1.app

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Gherkin: home_ui_readability.feature + compose_ui.feature
 * XML window theme stays high-contrast; ActionBar chrome is gone (NoActionBar).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThemeContrastTest {

    @Test
    fun buttonLabelContrastsWithNavyFill() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fill = ContextCompat.getColor(context, R.color.truckie_navy)
        val label = ContextCompat.getColor(context, R.color.truckie_on_primary)
        assertTrue(
            "Button label must contrast with navy fill (got ${contrastRatio(fill, label)})",
            contrastRatio(fill, label) >= 4.5,
        )
    }

    @Test
    fun bodyTextContrastsWithSkyBackground() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val text = ContextCompat.getColor(context, R.color.truckie_navy)
        val background = ContextCompat.getColor(context, R.color.truckie_sky)
        assertTrue(
            "Body text must contrast with sky background (got ${contrastRatio(text, background)})",
            contrastRatio(text, background) >= 4.5,
        )
    }

    @Test
    fun themeMapsPrimaryButtonsToOnPrimaryLabelWithoutActionBar() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val context = ContextThemeWrapper(base, R.style.Theme_Truckie)
        val theme = context.theme

        // Material 1.14+ no longer exposes colorPrimary on material.R; AppCompat owns the attr.
        val primaryColor = resolveThemeColor(theme, androidx.appcompat.R.attr.colorPrimary)
        val onPrimaryColor = resolveThemeColor(theme, com.google.android.material.R.attr.colorOnPrimary)

        assertTrue(
            "colorOnPrimary must contrast with colorPrimary",
            contrastRatio(primaryColor, onPrimaryColor) >= 4.5,
        )
        assertEquals(
            "Filled buttons use navy primary",
            ContextCompat.getColor(context, R.color.truckie_navy),
            primaryColor,
        )
        assertEquals(
            "Button labels use on-primary white",
            ContextCompat.getColor(context, R.color.truckie_on_primary),
            onPrimaryColor,
        )

        val windowActionBar = TypedValue()
        val resolved = theme.resolveAttribute(androidx.appcompat.R.attr.windowActionBar, windowActionBar, true)
        assertTrue("Theme must declare windowActionBar", resolved)
        assertEquals(
            "NoActionBar must disable the window ActionBar (edge-to-edge Compose chrome)",
            0,
            windowActionBar.data,
        )
    }

    private fun resolveThemeColor(theme: android.content.res.Resources.Theme, attr: Int): Int {
        val value = TypedValue()
        assertTrue("Missing theme attr $attr", theme.resolveAttribute(attr, value, true))
        return when {
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> value.data
            value.resourceId != 0 -> ContextCompat.getColor(
                ApplicationProvider.getApplicationContext(),
                value.resourceId,
            )
            else -> error("Theme attr $attr did not resolve to a color (type=${value.type})")
        }
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
        val r = channel(Color.red(color))
        val g = channel(Color.green(color))
        val b = channel(Color.blue(color))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
