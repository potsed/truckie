package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: map_first_home.feature — follow mode + Recenter when not centred.
 */
class MapLocationFollowTest {

    @Test
    fun followIsGatedByMapHomeAndLocationPuckFlags() {
        assertEquals("ff_map_home", FlagKeys.MAP_HOME)
        assertEquals("ff_map_location_puck", FlagKeys.MAP_LOCATION_PUCK)
        val off = LocalFeatureFlags()
        assertFalse(MapLocationFollow.isEnabled(off))
        val mapOnly = LocalFeatureFlags(mapOf(FlagKeys.MAP_HOME to true))
        assertFalse(MapLocationFollow.isEnabled(mapOnly))
        val puckOnly = LocalFeatureFlags(mapOf(FlagKeys.MAP_LOCATION_PUCK to true))
        assertFalse(MapLocationFollow.isEnabled(puckOnly))
        val both = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertTrue(MapLocationFollow.isEnabled(both))
    }

    @Test
    fun catalogDescribesFollowAndRecenterBehaviour() {
        assertTrue(MapLocationFollow.defaultsToFollowingWhenCentered)
        assertTrue(MapLocationFollow.puckLockedAtCentreWhileFollowing)
        assertTrue(MapLocationFollow.cameraTracksLocationWhileFollowing)
        assertTrue(MapLocationFollow.showsRecenterWhenNotFollowing)
        assertTrue(MapLocationFollow.hidesRecenterWhileFollowing)
        assertTrue(MapLocationFollow.resetsZoomToDefaultOnRecenter)
        assertEquals(MapCameraDefaults.ZOOM, MapLocationFollow.RECENTER_ZOOM, 0.0)
        assertEquals("TRACKING", MapLocationFollow.FOLLOWING_CAMERA_MODE)
        assertEquals("NONE", MapLocationFollow.FREE_CAMERA_MODE)
    }

    @Test
    fun recenterRestoresDefaultCloserZoomAndReEnablesFollow() {
        val free = MapLocationFollow.onUserPan(
            MapLocationFollow.initial(hasLocationFix = true),
        )
        assertTrue(free.showsRecenter)

        val result = MapLocationFollow.recenter(
            state = free,
            latitude = -27.4705,
            longitude = 153.0260,
        )

        assertTrue(result.followState.isFollowing)
        assertFalse(result.followState.showsRecenter)
        assertEquals(MapLocationFollow.FOLLOWING_CAMERA_MODE, result.followState.cameraMode)
        assertTrue(result.followState.shouldMoveCameraWithLocation)

        assertEquals(-27.4705, result.cameraTarget.latitude, 0.0)
        assertEquals(153.0260, result.cameraTarget.longitude, 0.0)
        assertEquals(
            "Recenter must restore the same closer zoom used when first centering on the puck",
            MapCameraDefaults.ZOOM,
            result.cameraTarget.zoom,
            0.0,
        )
        assertFalse(result.cameraTarget.usedFallback)
        assertTrue(result.resetsZoom)
    }

    @Test
    fun cameraTargetForRecenterAlwaysUsesDefaultCloserZoom() {
        val zoomedOutWouldBe = 11.0
        assertTrue(zoomedOutWouldBe < MapCameraDefaults.ZOOM)

        val target = MapLocationFollow.cameraTargetForRecenter(
            latitude = -33.8688,
            longitude = 151.2093,
        )
        assertEquals(MapCameraDefaults.ZOOM, target.zoom, 0.0)
        assertEquals(MapLocationFollow.RECENTER_ZOOM, target.zoom, 0.0)
        assertEquals(-33.8688, target.latitude, 0.0)
        assertEquals(151.2093, target.longitude, 0.0)
        assertFalse(target.usedFallback)
    }

    @Test
    fun initialStateIsFollowingWhenAFixIsAvailable() {
        val withFix = MapLocationFollow.initial(hasLocationFix = true)
        assertTrue(withFix.isFollowing)
        assertTrue(withFix.hasLocationFix)
        assertFalse(withFix.showsRecenter)
        assertEquals(MapLocationFollow.FOLLOWING_CAMERA_MODE, withFix.cameraMode)
        assertTrue(withFix.shouldMoveCameraWithLocation)

        val withoutFix = MapLocationFollow.initial(hasLocationFix = false)
        assertTrue(withoutFix.isFollowing)
        assertFalse(withoutFix.showsRecenter)
        assertFalse(withoutFix.shouldMoveCameraWithLocation)
    }

    @Test
    fun userPanExitsFollowAndShowsRecenterWhenFixAvailable() {
        val following = MapLocationFollow.initial(hasLocationFix = true)
        val free = MapLocationFollow.onUserPan(following)
        assertFalse(free.isFollowing)
        assertTrue(free.showsRecenter)
        assertEquals(MapLocationFollow.FREE_CAMERA_MODE, free.cameraMode)
        assertFalse(free.shouldMoveCameraWithLocation)
    }

    @Test
    fun userPanWithoutFixDoesNotShowRecenter() {
        val following = MapLocationFollow.initial(hasLocationFix = false)
        val free = MapLocationFollow.onUserPan(following)
        assertFalse(free.isFollowing)
        assertFalse(free.showsRecenter)
    }

    @Test
    fun recenterReEnablesFollowAndHidesControl() {
        val free = MapLocationFollow.onUserPan(
            MapLocationFollow.initial(hasLocationFix = true),
        )
        assertTrue(free.showsRecenter)
        val recentered = MapLocationFollow.onRecenter(free)
        assertTrue(recentered.isFollowing)
        assertFalse(recentered.showsRecenter)
        assertEquals(MapLocationFollow.FOLLOWING_CAMERA_MODE, recentered.cameraMode)
        assertTrue(recentered.shouldMoveCameraWithLocation)
    }

    @Test
    fun locationUpdatesKeepFollowModeButRefreshFixAvailability() {
        val followingNoFix = MapLocationFollow.initial(hasLocationFix = false)
        val withFix = MapLocationFollow.onLocationFixChanged(followingNoFix, hasLocationFix = true)
        assertTrue(withFix.isFollowing)
        assertTrue(withFix.hasLocationFix)
        assertFalse(withFix.showsRecenter)

        val free = MapLocationFollow.onUserPan(withFix)
        val lostFix = MapLocationFollow.onLocationFixChanged(free, hasLocationFix = false)
        assertFalse(lostFix.isFollowing)
        assertFalse(lostFix.showsRecenter)

        val regained = MapLocationFollow.onLocationFixChanged(lostFix, hasLocationFix = true)
        assertFalse(regained.isFollowing)
        assertTrue(regained.showsRecenter)
    }

    @Test
    fun catalogMarksFollowSupportOnMapHome() {
        assertTrue(MapHomeCatalog.supportsLocationFollow)
        assertTrue(MapHomeCatalog.supportsRecenterControl)
    }
}
