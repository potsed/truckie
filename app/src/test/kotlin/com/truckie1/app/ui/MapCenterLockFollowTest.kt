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
 * Gherkin: features/map_center_lock_follow.feature — ATNA-66.
 *
 * Center lock / follow must keep the puck map-centred while the device moves.
 * Recenter still uses one animateCamera path; follow enter must not use the
 * zero-duration timed setCameraMode overload that can stick MapLibre's
 * LocationCameraController.isTransitioning and block later location→camera updates.
 */
class MapCenterLockFollowTest {

    @Test
    fun centerLockUsesExistingFollowFlagsOnly() {
        assertEquals("ff_map_home", FlagKeys.MAP_HOME)
        assertEquals("ff_map_location_puck", FlagKeys.MAP_LOCATION_PUCK)
        val both = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertTrue(MapLocationFollow.isEnabled(both))
        assertTrue(MapLocationFollow.puckLockedAtCentreWhileFollowing)
        assertTrue(MapLocationFollow.cameraTracksLocationWhileFollowing)
    }

    @Test
    fun steadyFollowWhileMovingReliesOnTrackingNotGpsMoveCamera() {
        assertEquals(
            SteadyFollowCameraAction.RelyOnTracking,
            MapLocationFollow.steadyFollowCameraAction(
                isFollowing = true,
                hasLocationFix = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = true,
            ),
        )
        assertEquals(
            SteadyFollowCameraAction.EnableTracking,
            MapLocationFollow.steadyFollowCameraAction(
                isFollowing = true,
                hasLocationFix = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = false,
            ),
        )
        assertEquals(
            SteadyFollowCameraAction.None,
            MapLocationFollow.steadyFollowCameraAction(
                isFollowing = true,
                hasLocationFix = true,
                recenterInFlight = true,
                locationComponentAlreadyTracking = false,
            ),
        )
        assertEquals(
            SteadyFollowCameraAction.None,
            MapLocationFollow.steadyFollowCameraAction(
                isFollowing = false,
                hasLocationFix = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = false,
            ),
        )
        assertFalse(
            MapLocationFollow.shouldDriveCameraOnLocationFix(
                isFollowing = true,
                recenterInFlight = false,
                locationComponentAlreadyTracking = true,
            ),
        )
    }

    @Test
    fun followEnterAfterRecenterAvoidsZeroMsTimedOverload() {
        val enter = MapLocationFollow.followEnterAfterFramedRecenter()
        assertTrue(
            "Bare cameraMode setter + synced lastLocation uses MapLibre immediateAnimation",
            enter.usesBareCameraModeSetter,
        )
        assertTrue(
            "forceLocationUpdate before enter so lastLocation matches framed camera",
            enter.syncsLastLocationBeforeEnter,
        )
        assertFalse(enter.applyZoom)
        assertNull(enter.timedTransitionMs)
        assertFalse(
            "setCameraMode(mode, 0, null…) can leave isTransitioning stuck — puck moves, camera does not",
            enter.usesZeroDurationTimedTransitionOverload,
        )
        assertTrue(MapLocationFollow.followEnterAllowsLocationCameraUpdates(enter))
    }

    @Test
    fun zeroMsTimedOverloadIsRejectedAsUnsafeFollowEnter() {
        val stuck = FollowEnterSpec(
            usesBareCameraModeSetter = false,
            syncsLastLocationBeforeEnter = false,
            applyZoom = false,
            timedTransitionMs = 0L,
        )
        assertTrue(stuck.usesZeroDurationTimedTransitionOverload)
        assertFalse(MapLocationFollow.followEnterAllowsLocationCameraUpdates(stuck))
        assertTrue(MapLocationFollow.causesFollowEnterTransitionStickRisk(stuck))
    }

    @Test
    fun recenterPlanUsesSafeBareFollowEnter() {
        val plan = MapLocationFollow.cameraPlanForRecenter(
            latitude = -27.4705,
            longitude = 153.0260,
        )
        assertEquals(MapCameraDefaults.ZOOM, plan.target.zoom, 0.0)
        assertTrue(plan.followEnter.usesBareCameraModeSetter)
        assertTrue(plan.followEnter.syncsLastLocationBeforeEnter)
        assertFalse(plan.followEnter.applyZoom)
        assertNull(plan.followEnter.timedTransitionMs)
        assertTrue(MapLocationFollow.isSingleCameraPath(plan))
        assertFalse(MapLocationFollow.causesSecondZoom(plan))
        assertFalse(MapLocationFollow.causesFollowEnterTransitionStickRisk(plan.followEnter))

        val ops = MapLocationFollow.cameraOperations(plan)
        assertEquals(3, ops.size)
        assertEquals(RecenterCameraOp.ExitTracking, ops[0])
        val animate = ops[1] as RecenterCameraOp.AnimateToLatLngZoom
        assertEquals(MapLocationFollow.RECENTER_ANIMATION_MS, animate.durationMs)
        val enter = ops[2] as RecenterCameraOp.EnterFollow
        assertTrue(enter.spec.usesBareCameraModeSetter)
        assertTrue(enter.spec.syncsLastLocationBeforeEnter)
        assertNull(enter.spec.timedTransitionMs)
        assertFalse(enter.spec.applyZoom)
    }

    @Test
    fun headingUpStaysIndependentOfCenterLockPolicy() {
        assertEquals("TRACKING", MapLocationFollow.followingCameraMode(headingUp = false))
        assertEquals("TRACKING_GPS", MapLocationFollow.followingCameraMode(headingUp = true))
        val plan = MapLocationFollow.cameraPlanForRecenter(-27.0, 153.0)
        assertTrue(MapLocationFollow.isSingleCameraPath(plan))
        assertTrue(plan.followEnter.usesBareCameraModeSetter)
        // Orientation choice must not alter Recenter zoom or follow-enter safety.
        assertEquals(MapCameraDefaults.ZOOM, plan.target.zoom, 0.0)
        assertFalse(plan.followEnter.applyZoom)
    }

    @Test
    fun panExitsFollowAndRecenterRestoresCenteredTracking() {
        val following = MapLocationFollow.initial(hasLocationFix = true)
        assertTrue(following.isFollowing)
        val free = MapLocationFollow.onUserPan(following)
        assertFalse(free.isFollowing)
        assertTrue(free.showsRecenter)
        val result = MapLocationFollow.recenter(free, -27.4705, 153.0260)
        assertTrue(result.followState.isFollowing)
        assertFalse(result.followState.showsRecenter)
        assertTrue(MapLocationFollow.isSingleCameraPath(result.cameraPlan))
        assertTrue(result.cameraPlan.followEnter.usesBareCameraModeSetter)
        assertEquals("TRACKING", result.followState.cameraMode(headingUp = false))
        assertEquals("TRACKING_GPS", result.followState.cameraMode(headingUp = true))
    }
}
