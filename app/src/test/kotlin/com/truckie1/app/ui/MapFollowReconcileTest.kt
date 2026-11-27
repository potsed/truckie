package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/map_blue_dot_single_path.feature — ATNA-69.
 *
 * One reconcile path owns puck push, follow enter/exit, heading sync, and Recenter.
 * No competing SideEffect + GPS + AndroidView TRACKING enter paths.
 */
class MapFollowReconcileTest {

    private val fix = LocationFix(
        latitude = -27.4705,
        longitude = 153.0260,
        accuracyMeters = 8f,
        bearingDegrees = 45f,
    )

    private fun followingRuntime(
        tracking: Boolean = false,
        trackingIsHeadingUp: Boolean = false,
        headingUp: Boolean = false,
        recenterInFlight: Boolean = false,
    ) = MapFollowRuntime(
        isFollowing = true,
        hasPuckFix = true,
        showLocationPuck = true,
        locationFollowEnabled = true,
        headingUp = headingUp,
        recenterInFlight = recenterInFlight,
        tracking = tracking,
        trackingIsHeadingUp = trackingIsHeadingUp,
    )

    @Test
    fun usesExistingFollowFlagsOnly() {
        assertEquals("ff_map_home", FlagKeys.MAP_HOME)
        assertEquals("ff_map_location_puck", FlagKeys.MAP_LOCATION_PUCK)
        assertEquals("ff_map_heading_up", FlagKeys.MAP_HEADING_UP)
        val both = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.MAP_LOCATION_PUCK to true,
            ),
        )
        assertTrue(MapLocationFollow.isEnabled(both))
        assertTrue(MapFollowReconcile.singlePathOwnsPuckAndFollow)
    }

    @Test
    fun locationFixWhileNotTrackingPushesPuckOnceAndEntersFollow() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.LocationUpdated(
                fix = fix,
                camera = MapCameraTarget.resolve(fix.latitude, fix.longitude),
            ),
            runtime = followingRuntime(tracking = false),
        )
        assertEquals(
            listOf(
                MapFollowCommand.PushPuck(fix),
                MapFollowCommand.EnterFollow(
                    headingUp = false,
                    reason = FollowEnterReason.Bootstrap,
                ),
            ),
            commands,
        )
        assertFalse(commands.any { it is MapFollowCommand.HardCamera })
    }

    @Test
    fun steadyMotionWhileTrackingOnlyPushesPuck() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.LocationUpdated(
                fix = fix,
                camera = MapCameraTarget.resolve(fix.latitude, fix.longitude),
            ),
            runtime = followingRuntime(tracking = true),
        )
        assertEquals(listOf(MapFollowCommand.PushPuck(fix)), commands)
        assertFalse(commands.any { it is MapFollowCommand.EnterFollow })
        assertFalse(commands.any { it is MapFollowCommand.HardCamera })
    }

    @Test
    fun headingLockWhileFollowingSyncsModesWithoutExitingFollow() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.HeadingChanged(headingUp = true),
            runtime = followingRuntime(
                tracking = true,
                trackingIsHeadingUp = false,
                headingUp = true,
            ),
        )
        assertEquals(
            listOf(MapFollowCommand.SyncFollowModes(headingUp = true)),
            commands,
        )
        assertFalse(commands.any { it is MapFollowCommand.ExitFollow })
        assertFalse(commands.any { it is MapFollowCommand.RecenterAnimate })
    }

    @Test
    fun headingUnlockWhileFollowingSyncsBackToNorthUp() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.HeadingChanged(headingUp = false),
            runtime = followingRuntime(
                tracking = true,
                trackingIsHeadingUp = true,
                headingUp = false,
            ),
        )
        assertEquals(
            listOf(MapFollowCommand.SyncFollowModes(headingUp = false)),
            commands,
        )
    }

    @Test
    fun headingChangeIsNoOpWhenModesAlreadyMatch() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.HeadingChanged(headingUp = true),
            runtime = followingRuntime(
                tracking = true,
                trackingIsHeadingUp = true,
                headingUp = true,
            ),
        )
        assertTrue(commands.isEmpty())
    }

    @Test
    fun headingChangeIsNoOpDuringRecenterInFlight() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.HeadingChanged(headingUp = true),
            runtime = followingRuntime(
                tracking = true,
                trackingIsHeadingUp = false,
                headingUp = true,
                recenterInFlight = true,
            ),
        )
        assertTrue(commands.isEmpty())
    }

    @Test
    fun composeSyncDoesNotFightLiveRecenter() {
        // Live Recenter also has tracking=false; ComposeSync must not clear the arm
        // or re-enter follow mid-animate. Stuck arms clear via RecenterUnavailable.
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.ComposeSync,
            runtime = followingRuntime(
                tracking = false,
                recenterInFlight = true,
            ),
        )
        assertTrue(commands.isEmpty())
        assertFalse(commands.contains(MapFollowCommand.ClearRecenterArm))
        assertFalse(commands.any { it is MapFollowCommand.EnterFollow })
    }

    @Test
    fun stuckRecenterArmIsClearedWhenRecenterUnavailable() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.RecenterUnavailable,
            runtime = followingRuntime(recenterInFlight = true),
        )
        assertEquals(listOf(MapFollowCommand.ClearRecenterArm), commands)
    }

    @Test
    fun recenterRequestRunsSingleAnimatePath() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.RecenterRequested(
                latitude = fix.latitude,
                longitude = fix.longitude,
            ),
            runtime = followingRuntime(tracking = false).copy(isFollowing = true),
        )
        assertEquals(1, commands.size)
        val recenter = commands.single() as MapFollowCommand.RecenterAnimate
        assertEquals(fix.latitude, recenter.target.latitude, 0.0)
        assertEquals(fix.longitude, recenter.target.longitude, 0.0)
        assertEquals(MapCameraDefaults.ZOOM, recenter.target.zoom, 0.0)
        assertTrue(MapLocationFollow.isSingleCameraPath(recenter.plan))
        assertTrue(recenter.plan.followEnter.usesBareCameraModeSetter)
        assertNullSafe(recenter.plan.followEnter.timedTransitionMs)
    }

    @Test
    fun composeSyncDoesNotArmRecenterSeparately() {
        // SideEffect arming was a competing path that left ignoreTrackingDismiss stuck.
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.ComposeSync,
            runtime = followingRuntime(tracking = true),
        )
        assertFalse(commands.any { it is MapFollowCommand.ArmRecenter })
        assertTrue(commands.isEmpty() || commands.none { it is MapFollowCommand.RecenterAnimate })
    }

    @Test
    fun userPanExitsFollowOnce() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.UserPan,
            runtime = followingRuntime(tracking = true),
        )
        assertEquals(listOf(MapFollowCommand.ExitFollow), commands)
    }

    @Test
    fun followBecameFreeExitsTracking() {
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.FollowBecameFree,
            runtime = followingRuntime(tracking = true).copy(isFollowing = false),
        )
        assertEquals(listOf(MapFollowCommand.ExitFollow), commands)
    }

    @Test
    fun locationWithoutFollowUsesHardCameraOnly() {
        val camera = MapCameraTarget.resolve(fix.latitude, fix.longitude)
        val commands = MapFollowReconcile.reconcile(
            event = MapFollowEvent.LocationUpdated(fix = fix, camera = camera),
            runtime = followingRuntime().copy(
                locationFollowEnabled = false,
                isFollowing = false,
            ),
        )
        assertEquals(
            listOf(
                MapFollowCommand.PushPuck(fix),
                MapFollowCommand.HardCamera(camera),
            ),
            commands,
        )
    }

    @Test
    fun needsModeSyncWhenHeadingMismatchesTrackingMode() {
        assertTrue(
            MapFollowReconcile.needsModeSync(
                isFollowing = true,
                recenterInFlight = false,
                tracking = true,
                headingUpDesired = true,
                trackingIsHeadingUp = false,
            ),
        )
        assertFalse(
            MapFollowReconcile.needsModeSync(
                isFollowing = true,
                recenterInFlight = false,
                tracking = true,
                headingUpDesired = true,
                trackingIsHeadingUp = true,
            ),
        )
        assertFalse(
            MapFollowReconcile.needsModeSync(
                isFollowing = true,
                recenterInFlight = true,
                tracking = true,
                headingUpDesired = true,
                trackingIsHeadingUp = false,
            ),
        )
    }

    @Test
    fun mayEnterFollowBlockedOnlyDuringLiveRecenterUnlessFinish() {
        assertFalse(
            MapFollowReconcile.mayEnterFollow(
                recenterInFlight = true,
                fromRecenterFinish = false,
            ),
        )
        assertTrue(
            MapFollowReconcile.mayEnterFollow(
                recenterInFlight = true,
                fromRecenterFinish = true,
            ),
        )
        assertTrue(
            MapFollowReconcile.mayEnterFollow(
                recenterInFlight = false,
                fromRecenterFinish = false,
            ),
        )
    }

    @Test
    fun followEnterAlwaysUsesBareSetterNeverTimedOverload() {
        assertTrue(MapFollowReconcile.usesBareFollowEnterOnly)
        val enter = MapLocationFollow.followEnterAfterFramedRecenter()
        assertTrue(enter.usesBareCameraModeSetter)
        assertNullSafe(enter.timedTransitionMs)
        assertFalse(enter.usesZeroDurationTimedTransitionOverload)
    }

    private fun assertNullSafe(value: Long?) {
        assertEquals(null, value)
    }
}
