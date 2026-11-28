package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraTarget
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_home_orchestration.feature — thin MapLibreHost bind snapshot.
 */
class MapLibreBindSnapshotTest {

    @Test
    fun bridgeSpecsCarryPrecomputedLabelsWithoutAppLookup() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.VERIFIED_CBD_CLEARANCES to true))
        val bridge = BridgePoint(
            id = "story-bridge-sample",
            name = "Story Bridge",
            latitude = -27.4636,
            longitude = 153.0351,
            clearanceHeightM = 3.5,
        )
        val label = BridgeMarkersCatalog.clearanceLabel(bridge, flags)
        val specs = MapLibreBindSnapshot.bridgeSpecs(
            bridges = listOf(bridge),
            flags = flags,
        )
        assertEquals(1, specs.size)
        assertEquals(bridge, specs.single().bridge)
        assertEquals(label, specs.single().clearanceLabel)
        assertTrue(MapHomeOrchestrationCatalog.hostDoesNotLookupAppFlags)
        assertTrue(MapHomeOrchestrationCatalog.hostBindsPrecomputedSnapshot)
    }

    @Test
    fun snapshotHoldsStopsPathAndCameraForOneApply() {
        val stop = RouteStop(
            id = "s1",
            addressLabel = "Yard",
            kind = StopKind.PICKUP_LOAD,
            latitude = -27.4,
            longitude = 153.0,
        )
        val path = listOf(LatLngPoint(-27.4, 153.0), LatLngPoint(-27.5, 153.1))
        val snapshot = MapLibreBindSnapshot(
            stops = listOf(stop),
            routePathPoints = path,
            bridges = emptyList(),
            prioritiseStreetLabels = true,
            showLocationPuck = true,
            locationFollowEnabled = true,
            headingUpEnabled = false,
            locationFix = LocationFix(latitude = -27.4, longitude = 153.0),
            cameraTarget = MapCameraTarget.fallback(),
            isFollowing = true,
        )
        assertEquals(1, snapshot.stops.size)
        assertEquals(2, snapshot.routePathPoints.size)
        assertTrue(snapshot.prioritiseStreetLabels)
        assertTrue(snapshot.showLocationPuck)
        assertFalse(snapshot.headingUpEnabled)
        assertTrue(snapshot.isFollowing)
    }
}
