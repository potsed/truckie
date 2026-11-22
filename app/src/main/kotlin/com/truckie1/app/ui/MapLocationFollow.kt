package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget

/**
 * Follow-mode + Recenter for map-home location (`ff_map_home` + `ff_map_location_puck`).
 *
 * Default when centred: puck stays locked at map centre and the camera tracks
 * Fused Location updates. Panning exits follow and shows Recenter; tapping
 * Recenter animates back onto the fix, restores the default closer zoom, and
 * re-enables follow.
 *
 * Gherkin: features/map_first_home.feature
 */
object MapLocationFollow {
    const val defaultsToFollowingWhenCentered: Boolean = true
    const val puckLockedAtCentreWhileFollowing: Boolean = true
    const val cameraTracksLocationWhileFollowing: Boolean = true
    const val showsRecenterWhenNotFollowing: Boolean = true
    const val hidesRecenterWhileFollowing: Boolean = true

    /**
     * Recenter restores the same street-neighbourhood zoom used when first
     * centering on the my-location puck ([MapCameraDefaults.ZOOM]).
     */
    const val resetsZoomToDefaultOnRecenter: Boolean = true

    /** Default closer zoom applied on Recenter (same as initial location center). */
    val RECENTER_ZOOM: Double = MapCameraDefaults.ZOOM

    /** MapLibre LocationComponent camera mode while following. */
    const val FOLLOWING_CAMERA_MODE: String = "TRACKING"

    /** MapLibre LocationComponent camera mode after the driver pans away. */
    const val FREE_CAMERA_MODE: String = "NONE"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_HOME) && flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK)

    fun initial(hasLocationFix: Boolean = false): MapFollowState =
        MapFollowState(
            mode = MapFollowMode.FOLLOWING,
            hasLocationFix = hasLocationFix,
        )

    fun onUserPan(state: MapFollowState): MapFollowState =
        state.copy(mode = MapFollowMode.FREE)

    fun onRecenter(state: MapFollowState): MapFollowState =
        state.copy(mode = MapFollowMode.FOLLOWING)

    /**
     * Camera target for Recenter: current fix + default closer zoom
     * ([RECENTER_ZOOM] / [MapCameraDefaults.ZOOM]), never the prior free-pan zoom.
     */
    fun cameraTargetForRecenter(latitude: Double, longitude: Double): MapCameraTarget =
        MapCameraTarget.resolve(latitude, longitude, zoom = RECENTER_ZOOM)

    /**
     * Recenter: re-enable follow and restore default closer zoom onto [latitude]/[longitude].
     */
    fun recenter(
        state: MapFollowState,
        latitude: Double,
        longitude: Double,
    ): RecenterResult =
        RecenterResult(
            followState = onRecenter(state),
            cameraTarget = cameraTargetForRecenter(latitude, longitude),
            resetsZoom = resetsZoomToDefaultOnRecenter,
            cameraPlan = cameraPlanForRecenter(latitude, longitude),
        )

    /**
     * MapLibre apply plan for Recenter. Prefer [RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM]:
     * CameraMode.TRACKING owns the camera and keeps the zoom current when TRACKING starts,
     * so async [org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom] is discarded
     * when follow is re-enabled immediately (#24 regression).
     */
    fun cameraPlanForRecenter(latitude: Double, longitude: Double): RecenterCameraPlan =
        RecenterCameraPlan(
            target = cameraTargetForRecenter(latitude, longitude),
            zoomApplyMode = RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM,
            animationDurationMs = RECENTER_ANIMATION_MS,
        )

    /**
     * Models MapLibre Recenter zoom outcome for tests.
     * Async animateCamera + immediate TRACKING keeps [priorZoom]; tracking-transition
     * (or sync moveCamera before TRACKING) applies [RecenterCameraPlan] zoom.
     */
    fun effectiveZoomAfterRecenter(
        plan: RecenterCameraPlan,
        priorZoom: Double,
        trackingEnabledImmediately: Boolean = true,
    ): Double = when (plan.zoomApplyMode) {
        RecenterZoomApplyMode.ANIMATE_CAMERA_LAT_LNG_ZOOM ->
            if (trackingEnabledImmediately) priorZoom else plan.target.zoom
        RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM -> plan.target.zoom
    }

    fun onLocationFixChanged(state: MapFollowState, hasLocationFix: Boolean): MapFollowState =
        state.copy(hasLocationFix = hasLocationFix)

    /** Duration shared with MapLibreHost Recenter animation. */
    const val RECENTER_ANIMATION_MS: Long = 750L
}

/** Outcome of tapping Recenter: follow restored + camera at default closer zoom. */
data class RecenterResult(
    val followState: MapFollowState,
    val cameraTarget: MapCameraTarget,
    val resetsZoom: Boolean,
    val cameraPlan: RecenterCameraPlan,
)

/**
 * How default closer zoom is applied under MapLibre LocationComponent TRACKING.
 */
enum class RecenterZoomApplyMode {
    /**
     * Broken with immediate TRACKING (#24): async animateCamera zoom never lands.
     */
    ANIMATE_CAMERA_LAT_LNG_ZOOM,

    /**
     * Correct: [org.maplibre.android.location.LocationComponent.setCameraMode] with
     * zoom transition, or sync moveCamera(newLatLngZoom) before enabling TRACKING.
     */
    TRACKING_TRANSITION_WITH_ZOOM,
}

/** Camera ops Recenter must perform so MapLibre honors [MapCameraDefaults.ZOOM]. */
data class RecenterCameraPlan(
    val target: MapCameraTarget,
    val zoomApplyMode: RecenterZoomApplyMode,
    val animationDurationMs: Long,
)

enum class MapFollowMode {
    FOLLOWING,
    FREE,
}

data class MapFollowState(
    val mode: MapFollowMode = MapFollowMode.FOLLOWING,
    val hasLocationFix: Boolean = false,
) {
    val isFollowing: Boolean get() = mode == MapFollowMode.FOLLOWING

    val showsRecenter: Boolean
        get() = hasLocationFix && mode == MapFollowMode.FREE &&
            MapLocationFollow.showsRecenterWhenNotFollowing

    val cameraMode: String
        get() = if (isFollowing) {
            MapLocationFollow.FOLLOWING_CAMERA_MODE
        } else {
            MapLocationFollow.FREE_CAMERA_MODE
        }

    val shouldMoveCameraWithLocation: Boolean
        get() = isFollowing &&
            hasLocationFix &&
            MapLocationFollow.cameraTracksLocationWhileFollowing
}
