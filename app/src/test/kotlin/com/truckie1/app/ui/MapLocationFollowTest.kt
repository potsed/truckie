package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals("TRACKING_GPS", MapLocationFollow.FOLLOWING_HEADING_UP_CAMERA_MODE)
        assertEquals("NORMAL", MapLocationFollow.FOLLOWING_RENDER_MODE)
        assertEquals("GPS", MapLocationFollow.FOLLOWING_HEADING_UP_RENDER_MODE)
        assertEquals("NONE", MapLocationFollow.FREE_CAMERA_MODE)
    }

    @Test
    fun headingUpUsesTrackingGpsWhileFollowing() {
        assertEquals("ff_map_heading_up", FlagKeys.MAP_HEADING_UP)
        val off = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertFalse(MapLocationFollow.isHeadingUpEnabled(off))
        assertEquals("TRACKING", MapLocationFollow.followingCameraMode(headingUp = false))
        assertEquals("NORMAL", MapLocationFollow.followingRenderMode(headingUp = false))

        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
                FlagKeys.MAP_HEADING_UP to true,
            ),
        )
        assertTrue(MapLocationFollow.isHeadingUpEnabled(on))
        assertEquals("TRACKING_GPS", MapLocationFollow.followingCameraMode(headingUp = true))
        assertEquals("GPS", MapLocationFollow.followingRenderMode(headingUp = true))

        val following = MapLocationFollow.initial(hasLocationFix = true)
        assertEquals("TRACKING_GPS", following.cameraMode(headingUp = true))
        assertEquals("TRACKING", following.cameraMode(headingUp = false))
        val free = MapLocationFollow.onUserPan(following)
        assertEquals("NONE", free.cameraMode(headingUp = true))
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
        assertTrue(MapLocationFollow.isSingleCameraPath(result.cameraPlan))
        assertFalse(MapLocationFollow.causesSecondZoom(result.cameraPlan))
        assertFalse(MapLocationFollow.usesHardCameraJump(result.cameraPlan))
        assertEquals(MapCameraDefaults.ZOOM, result.cameraPlan.target.zoom, 0.0)
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
    fun recenterUsesSingleAnimateThenZeroDurationFollow() {
        // ATNA-61: one animateCamera; MapLibre bare setCameraMode defaults to 750ms
        // transition which was the #44 second zoom + off-centre.
        val plan = MapLocationFollow.cameraPlanForRecenter(
            latitude = -27.4705,
            longitude = 153.0260,
        )
        assertEquals(MapCameraDefaults.ZOOM, plan.target.zoom, 0.0)
        assertEquals(MapLocationFollow.RECENTER_ANIMATION_MS, plan.animationDurationMs)
        assertEquals(0L, plan.followEnterTransitionMs)
        assertFalse(plan.applyZoomOnFollowEnter)
        assertTrue(MapLocationFollow.isSingleCameraPath(plan))
        assertFalse(MapLocationFollow.causesSecondZoom(plan))
        assertFalse(MapLocationFollow.usesHardCameraJump(plan))

        val ops = MapLocationFollow.cameraOperations(plan)
        assertEquals(3, ops.size)
        assertEquals(RecenterCameraOp.ExitTracking, ops[0])
        val animate = ops[1] as RecenterCameraOp.AnimateToLatLngZoom
        assertEquals(-27.4705, animate.latitude, 0.0)
        assertEquals(153.0260, animate.longitude, 0.0)
        assertEquals(MapCameraDefaults.ZOOM, animate.zoom, 0.0)
        assertEquals(MapLocationFollow.RECENTER_ANIMATION_MS, animate.durationMs)
        val enter = ops[2] as RecenterCameraOp.EnterFollow
        assertEquals(0L, enter.transitionMs)
        assertNull(enter.zoom)

        // #44 regression model: default 750ms follow-enter = second zoom.
        val doubleZoom = plan.copy(followEnterTransitionMs = 750L)
        assertTrue(MapLocationFollow.causesSecondZoom(doubleZoom))
        assertFalse(MapLocationFollow.isSingleCameraPath(doubleZoom))
        val zoomOnEnter = plan.copy(applyZoomOnFollowEnter = true)
        assertTrue(MapLocationFollow.causesSecondZoom(zoomOnEnter))
    }

    @Test
    fun steadyFollowDoesNotDriveCameraOnGpsTicks() {
        // LocationComponent owns the camera while tracking — GPS moveCamera was a
        // second path that fought Recenter (off-centre / extra zoom).
        assertFalse(
            MapLocationFollow.shouldDriveCameraOnLocationFix(
                isFollowing = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = true,
            ),
        )
        assertFalse(
            MapLocationFollow.shouldDriveCameraOnLocationFix(
                isFollowing = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = false,
            ),
        )
        assertTrue(
            MapLocationFollow.shouldEnableFollowMode(
                isFollowing = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = false,
            ),
        )
        assertFalse(
            MapLocationFollow.shouldEnableFollowMode(
                isFollowing = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = true,
            ),
        )
        assertFalse(
            MapLocationFollow.shouldEnableFollowMode(
                isFollowing = true,
                recenterInFlight = true,
                locationComponentAlreadyTracking = false,
            ),
        )
    }

    @Test
    fun recenterInFlightBlocksBareFollowSync() {
        assertFalse(
            MapLocationFollow.shouldSyncFollowingModeWithoutZoom(
                isFollowing = true,
                recenterInFlight = true,
            ),
        )
        assertTrue(
            MapLocationFollow.shouldSyncFollowingModeWithoutZoom(
                isFollowing = true,
                recenterInFlight = false,
            ),
        )
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
