package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
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
        assertEquals("TRACKING", MapLocationFollow.FOLLOWING_CAMERA_MODE)
        assertEquals("NONE", MapLocationFollow.FREE_CAMERA_MODE)
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
