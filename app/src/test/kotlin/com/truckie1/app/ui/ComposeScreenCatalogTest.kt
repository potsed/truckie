package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: compose_ui.feature — every screen is Compose-hosted; XML layouts gone.
 * ATNA-74: sole host is MainActivity NavHost.
 */
class ComposeScreenCatalogTest {

    @Test
    fun soleScreenActivityIsMainActivity() {
        assertEquals(
            setOf("MainActivity"),
            ComposeScreenCatalog.activitySimpleNames.toSet(),
        )
        LegacyActivityRetirementCatalog.deletedActivitySimpleNames.forEach { name ->
            assertFalse(ComposeScreenCatalog.activitySimpleNames.contains(name))
        }
    }

    @Test
    fun obsoleteLayoutResourceNamesAreListedForDeletion() {
        val layouts = ComposeScreenCatalog.deletedLayoutResourceNames
        assertTrue(layouts.contains("activity_main"))
        assertTrue(layouts.contains("activity_vehicle_list"))
        assertTrue(layouts.contains("activity_vehicle_edit"))
        assertTrue(layouts.contains("activity_trip_stops"))
        assertTrue(layouts.contains("activity_add_stop"))
        assertTrue(layouts.contains("activity_blocked_route"))
        assertTrue(layouts.contains("item_vehicle_row"))
        assertTrue(layouts.contains("item_trip_stop_row"))
    }

    @Test
    fun themeRequiresNoActionBar() {
        assertFalse(ComposeScreenCatalog.usesActionBar)
        assertTrue(ComposeScreenCatalog.usesEdgeToEdgeInsets)
    }
}
