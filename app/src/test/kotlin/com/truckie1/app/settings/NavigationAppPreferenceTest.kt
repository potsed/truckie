package com.truckie1.app.settings

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: create_route_start_end.feature — Settings nav-app preference.
 */
class NavigationAppPreferenceTest {

    @Test
    fun preferenceKeyAndInAppDefault() {
        assertEquals("navigation_app_package", NavigationAppPreference.KEY)
        assertEquals("com.truckie1.in_app", NavigationAppPreference.VALUE_IN_APP)
        val store = InMemoryNavigationAppStore()
        assertEquals(NavigationAppSelection.InAppTruckie, store.selection())
        assertTrue(store.selection().isInApp)
    }

    @Test
    fun persistsExternalSelection() {
        val store = InMemoryNavigationAppStore()
        store.setSelection(
            NavigationAppSelection.External(
                packageName = "com.google.android.apps.maps",
                label = "Google Maps",
            ),
        )
        assertEquals("com.google.android.apps.maps", store.packageName())
        assertFalse(store.selection().isInApp)
        store.setSelection(NavigationAppSelection.InAppTruckie)
        assertEquals(NavigationAppPreference.VALUE_IN_APP, store.packageName())
        assertTrue(store.selection().isInApp)
    }

    @Test
    fun listsInstalledAppsViaQueryWrapper() {
        val query = InstalledNavigationAppQuery {
            listOf(
                InstalledNavigationApp("com.google.android.apps.maps", "Google Maps"),
                InstalledNavigationApp("com.example.nav", "Example Nav"),
            )
        }
        val options = NavigationAppPreference.options(query)
        assertEquals(3, options.size)
        assertEquals(NavigationAppSelection.InAppTruckie, options.first())
        assertEquals("com.google.android.apps.maps", (options[1] as NavigationAppSelection.External).packageName)
        assertEquals("Example Nav", (options[2] as NavigationAppSelection.External).label)
    }

    @Test
    fun settingsSectionGatedByNavigateFlag() {
        val flags = LocalFeatureFlags()
        assertFalse(NavigationAppPreference.showsInSettings(flags))
        flags.setEnabled(FlagKeys.NAVIGATE, true)
        assertTrue(NavigationAppPreference.showsInSettings(flags))
    }
}
