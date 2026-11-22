package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: compose_ui.feature — every screen Activity is Compose-hosted; XML layouts gone.
 */
class ComposeScreenCatalogTest {

    @Test
    fun allScreenActivitiesAreCatalogued() {
        assertEquals(
            setOf(
                "MainActivity",
                "VehicleListActivity",
                "VehicleEditActivity",
                "TripStopsActivity",
                "AddStopActivity",
                "BlockedRouteActivity",
            ),
            ComposeScreenCatalog.activitySimpleNames.toSet(),
        )
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
