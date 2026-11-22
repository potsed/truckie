package com.truckie1.app.ui

import android.Manifest
import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — Fused Location Provider + permission / Settings UX.
 */
class MapLocationPermissionCatalogTest {

    @Test
    fun requestsFineAndCoarseLocationForMapCamera() {
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            MapHomeCatalog.locationPermissions,
        )
        assertTrue(MapHomeCatalog.usesCurrentLocationCamera)
        assertTrue(MapHomeCatalog.fallsBackToBrisbaneWhenLocationUnavailable)
    }

    @Test
    fun usesOfficialFusedLocationProviderStack() {
        assertTrue(MapHomeCatalog.usesFusedLocationProvider)
        assertTrue(MapHomeCatalog.usesPlayServicesLocationSettingsResolution)
    }

    @Test
    fun locationSettingsResolutionTreatsResultOkAsSuccess() {
        assertTrue(locationSettingsResolutionSucceeded(Activity.RESULT_OK))
        assertFalse(locationSettingsResolutionSucceeded(Activity.RESULT_CANCELED))
    }
}
