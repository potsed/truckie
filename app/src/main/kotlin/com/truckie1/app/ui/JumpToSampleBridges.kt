package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget

/**
 * TEMPORARY / DEV helpers: fly the MapLibre camera to the SEQ sample bridge
 * cluster near Brisbane CBD (`au_sample_bridges.json` / pack 0.7.0-sample).
 *
 * ATNA-67: the floating map FAB is **removed**. [showsFloatingMapControl] is
 * always false; Demo mode stays in Settings. Camera helpers remain for unit
 * tests (and any future non-FAB entry point). Does **not** expand the AU dataset.
 *
 * Gherkin: features/jump_to_sample_bridges.feature
 */
object JumpToSampleBridges {
    /** Marked temporary so dogfood / PR reviewers treat this as short-lived. */
    const val isTemporaryDevAffordance: Boolean = true

    /** Map chrome FAB removed — do not reintroduce without a product ask. */
    const val showsFloatingMapControl: Boolean = false

    const val SHORT_LABEL: String = "Jump to sample bridges"
    const val TEST_TAG: String = "map_jump_to_sample_bridges"

    /**
     * Wider than street-neighbourhood [MapCameraDefaults.ZOOM] so several W5-8
     * markers within ~2 km of CBD are visible together.
     */
    const val ZOOM: Double = 13.5

    const val ANIMATION_MS: Long = 900L

    /**
     * Always false: floating Jump control is gone from map-home.
     * Demo mode + bridge markers still gate markers/data elsewhere.
     */
    fun shouldShow(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean {
        if (!showsFloatingMapControl) return false
        return mapHomeEnabled &&
            DemoModeCatalog.isEnabled(flags) &&
            BridgeMarkersCatalog.isEnabled(flags)
    }

    /** Brisbane CBD fallback — centre of the SEQ sample cluster. */
    fun cameraTarget(): MapCameraTarget =
        MapCameraTarget(
            latitude = MapCameraDefaults.FALLBACK_LATITUDE,
            longitude = MapCameraDefaults.FALLBACK_LONGITUDE,
            zoom = ZOOM,
            usedFallback = true,
        )

    /**
     * Exits follow so Fused Location updates do not yank the camera away from
     * the sample cluster after the fly animation.
     */
    fun onJump(followState: MapFollowState): JumpToSampleBridgesResult =
        JumpToSampleBridgesResult(
            followState = MapLocationFollow.onUserPan(followState),
            cameraTarget = cameraTarget(),
            animatesCamera = true,
            animationDurationMs = ANIMATION_MS,
        )
}

data class JumpToSampleBridgesResult(
    val followState: MapFollowState,
    val cameraTarget: MapCameraTarget,
    val animatesCamera: Boolean,
    val animationDurationMs: Long,
)
