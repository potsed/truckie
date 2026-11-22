package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/jump_to_sample_bridges.feature
 *
 * TEMPORARY / DEV: fly camera to SEQ sample bridges near Brisbane CBD.
 * Gated by ff_demo_mode (Settings) + ff_bridge_markers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JumpToSampleBridgesTest {

    @Test
    fun catalogIsTemporaryDevAffordanceBehindDemoModeAndBridgeMarkers() {
        assertTrue(JumpToSampleBridges.isTemporaryDevAffordance)
        val bridgesOnly = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.BRIDGE_MARKERS to true,
            ),
        )
        assertFalse(JumpToSampleBridges.shouldShow(bridgesOnly, mapHomeEnabled = true))

        val demoOnly = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.DEMO_MODE to true,
            ),
        )
        assertFalse(JumpToSampleBridges.shouldShow(demoOnly, mapHomeEnabled = true))

        assertFalse(
            JumpToSampleBridges.shouldShow(
                LocalFeatureFlags(
                    mapOf(
                        FlagKeys.BRIDGE_MARKERS to true,
                        FlagKeys.DEMO_MODE to true,
                    ),
                ),
                mapHomeEnabled = false,
            ),
        )

        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.DEMO_MODE to true,
            ),
        )
        assertTrue(JumpToSampleBridges.shouldShow(on, mapHomeEnabled = true))
    }

    @Test
    fun turningDemoModeOffHidesJumpEvenWhenBridgesFlagOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.MAP_HOME to true,
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.DEMO_MODE to true,
            ),
        )
        assertTrue(JumpToSampleBridges.shouldShow(flags, mapHomeEnabled = true))
        DemoModeCatalog.setEnabled(flags, enabled = false)
        assertFalse(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertFalse(JumpToSampleBridges.shouldShow(flags, mapHomeEnabled = true))
    }

    @Test
    fun cameraTargetsBrisbaneCbdSampleClusterAtZoomShowingSeveralMarkers() {
        val target = JumpToSampleBridges.cameraTarget()
        assertEquals(MapCameraDefaults.FALLBACK_LATITUDE, target.latitude, 0.0001)
        assertEquals(MapCameraDefaults.FALLBACK_LONGITUDE, target.longitude, 0.0001)
        assertTrue(
            "Zoom must be wider than street-neighbourhood so several W5-8 markers fit " +
                "(was ${target.zoom})",
            target.zoom < MapCameraDefaults.ZOOM,
        )
        assertTrue(
            "Zoom should stay neighbourhood-readable for W5-8 (12.5–14.5), was ${target.zoom}",
            target.zoom in 12.5..14.5,
        )
        assertEquals(JumpToSampleBridges.ZOOM, target.zoom, 0.001)
        assertTrue(target.usedFallback)
    }

    @Test
    fun onJumpExitsFollowAndRequestsCameraAnimation() {
        val following = MapLocationFollow.initial(hasLocationFix = true)
        assertTrue(following.isFollowing)
        val result = JumpToSampleBridges.onJump(following)
        assertFalse(result.followState.isFollowing)
        assertTrue(result.followState.showsRecenter)
        assertEquals(
            MapCameraDefaults.FALLBACK_LATITUDE,
            result.cameraTarget.latitude,
            0.0001,
        )
        assertEquals(
            MapCameraDefaults.FALLBACK_LONGITUDE,
            result.cameraTarget.longitude,
            0.0001,
        )
        assertEquals(JumpToSampleBridges.ZOOM, result.cameraTarget.zoom, 0.001)
        assertTrue(result.animatesCamera)
        assertEquals(JumpToSampleBridges.ANIMATION_MS, result.animationDurationMs)
    }

    @Test
    fun labelIsShortJumpToSampleBridges() {
        assertEquals("Jump to sample bridges", JumpToSampleBridges.SHORT_LABEL)
        assertEquals("map_jump_to_sample_bridges", JumpToSampleBridges.TEST_TAG)
    }
}
