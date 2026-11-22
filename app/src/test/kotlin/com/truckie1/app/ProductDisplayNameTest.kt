package com.truckie1.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin / product rename: user-facing brand is Truckie (not Truckie1).
 * Package namespace remains com.truckie1 until a separate migration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProductDisplayNameTest {

    @Test
    fun appLabelAndHomeTitleAreTruckie() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Truckie", context.getString(R.string.app_name))
        assertEquals("Truckie", context.getString(R.string.home_title))
    }

    @Test
    fun windowThemeResourceIsThemeTruckie() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Generated style id must resolve after Theme.Truckie1 → Theme.Truckie rename.
        val styleId = R.style.Theme_Truckie
        assertEquals(
            "Theme.Truckie",
            context.resources.getResourceEntryName(styleId),
        )
    }
}
