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
 * With [ff_map_heading_up][FlagKeys.MAP_HEADING_UP], follow uses course-up
 * (MapLibre `TRACKING_GPS`) so the map orients to the direction of travel.
 *
 * Gherkin: features/map_first_home.feature, features/map_heading_up.feature
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

    /** MapLibre LocationComponent camera mode while following (north-up). */
    const val FOLLOWING_CAMERA_MODE: String = "TRACKING"

    /**
     * Course-up / heading-up while following (`ff_map_heading_up`):
     * MapLibre [org.maplibre.android.location.modes.CameraMode.TRACKING_GPS].
     */
    const val FOLLOWING_HEADING_UP_CAMERA_MODE: String = "TRACKING_GPS"

    /** MapLibre LocationComponent camera mode after the driver pans away. */
    const val FREE_CAMERA_MODE: String = "NONE"

    /** Puck render while north-up following. */
    const val FOLLOWING_RENDER_MODE: String = "NORMAL"

    /** Puck render while heading-up — chevron / GPS direction. */
    const val FOLLOWING_HEADING_UP_RENDER_MODE: String = "GPS"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_HOME) && flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK)

    fun isHeadingUpEnabled(flags: FeatureFlags): Boolean =
        isEnabled(flags) && flags.isEnabled(FlagKeys.MAP_HEADING_UP)

    fun followingCameraMode(headingUp: Boolean): String =
        if (headingUp) FOLLOWING_HEADING_UP_CAMERA_MODE else FOLLOWING_CAMERA_MODE

    fun followingRenderMode(headingUp: Boolean): String =
        if (headingUp) FOLLOWING_HEADING_UP_RENDER_MODE else FOLLOWING_RENDER_MODE

    /** MapLibre [CameraMode] int for follow (tests / host wiring). */
    fun followingCameraModeInt(headingUp: Boolean): Int =
        if (headingUp) {
            org.maplibre.android.location.modes.CameraMode.TRACKING_GPS
        } else {
            org.maplibre.android.location.modes.CameraMode.TRACKING
        }

    fun followingRenderModeInt(headingUp: Boolean): Int =
        if (headingUp) {
            org.maplibre.android.location.modes.RenderMode.GPS
        } else {
            org.maplibre.android.location.modes.RenderMode.NORMAL
        }

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
     * MapLibre apply plan for Recenter (ATNA-60).
     *
     * Prefer [RecenterZoomApplyMode.ANIMATE_THEN_FOLLOW]: leave TRACKING, smoothly
     * [org.maplibre.android.maps.MapLibreMap.animateCamera] to lat/lng + default closer
     * zoom, then enable follow when the animation finishes. Do **not** sync
     * [moveCamera] first — that hard-jumps (ATNA-59 #43 regression feel).
     *
     * #24: enabling TRACKING immediately after async animateCamera kept free-pan zoom;
     * enabling follow only in onFinish fixes that while staying smooth.
     */
    fun cameraPlanForRecenter(latitude: Double, longitude: Double): RecenterCameraPlan =
        RecenterCameraPlan(
            target = cameraTargetForRecenter(latitude, longitude),
            zoomApplyMode = RecenterZoomApplyMode.ANIMATE_THEN_FOLLOW,
            cameraMotion = RecenterCameraMotion.SMOOTH_ANIMATE,
            animationDurationMs = RECENTER_ANIMATION_MS,
            exitTrackingBeforeApply = true,
            syncJumpBeforeFollow = false,
            enableFollowAfterAnimation = true,
        )

    /**
     * Models MapLibre Recenter zoom outcome for tests.
     */
    fun effectiveZoomAfterRecenter(
        plan: RecenterCameraPlan,
        priorZoom: Double,
        trackingEnabledImmediately: Boolean = true,
        trackingAlreadyActiveWithoutZoom: Boolean = false,
    ): Double = when (plan.zoomApplyMode) {
        RecenterZoomApplyMode.ANIMATE_CAMERA_LAT_LNG_ZOOM ->
            if (trackingEnabledImmediately) priorZoom else plan.target.zoom
        RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM -> {
            if (trackingAlreadyActiveWithoutZoom && !plan.exitTrackingBeforeApply) {
                priorZoom
            } else {
                plan.target.zoom
            }
        }
        RecenterZoomApplyMode.ANIMATE_THEN_FOLLOW ->
            if (plan.enableFollowAfterAnimation && trackingEnabledImmediately) {
                // Follow enabled too early → #24 style failure.
                priorZoom
            } else {
                plan.target.zoom
            }
    }

    /** Hard jump = sync moveCamera before follow (ATNA-60 forbids this for Recenter). */
    fun usesHardCameraJump(plan: RecenterCameraPlan): Boolean =
        plan.syncJumpBeforeFollow || plan.cameraMotion == RecenterCameraMotion.HARD_JUMP

    /**
     * Heading-up SideEffect must not call bare setCameraFollowing during Recenter —
     * that re-enters TRACKING without zoom and fights the zoom transition (ATNA-59).
     */
    fun shouldSyncFollowingModeWithoutZoom(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
    ): Boolean = isFollowing && !recenterInFlight

    /**
     * While Recenter animation is in flight, do not re-apply follow camera on GPS ticks.
     */
    fun shouldApplyFollowCameraOnLocationFix(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): Boolean {
        if (recenterInFlight) return false
        if (!isFollowing) return false
        return !locationComponentAlreadyTracking
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
     * setCameraMode(..., zoom) transition; often paired with sync moveCamera (hard jump).
     */
    TRACKING_TRANSITION_WITH_ZOOM,

    /**
     * Preferred (ATNA-60): animateCamera pan+zoom, then enable follow on finish.
     */
    ANIMATE_THEN_FOLLOW,
}

/** Whether Recenter moves the camera with a smooth animation or a hard jump. */
enum class RecenterCameraMotion {
    /** Sync moveCamera / instant cameraPosition — feels like a jump (ATNA-60 forbids). */
    HARD_JUMP,

    /** animateCamera over [RecenterCameraPlan.animationDurationMs]. */
    SMOOTH_ANIMATE,
}

/** Camera ops Recenter must perform so MapLibre honors [MapCameraDefaults.ZOOM]. */
data class RecenterCameraPlan(
    val target: MapCameraTarget,
    val zoomApplyMode: RecenterZoomApplyMode,
    val animationDurationMs: Long,
    val cameraMotion: RecenterCameraMotion = RecenterCameraMotion.SMOOTH_ANIMATE,
    /**
     * When true, drop to [org.maplibre.android.location.modes.CameraMode.NONE] before
     * animating so LocationComponent does not own the camera mid-flight.
     */
    val exitTrackingBeforeApply: Boolean = true,
    /**
     * ATNA-59 used sync moveCamera before follow for zoom reliability — that hard-jumps.
     * ATNA-60 requires false.
     */
    val syncJumpBeforeFollow: Boolean = false,
    /** Enable TRACKING only after animateCamera onFinish (not immediately). */
    val enableFollowAfterAnimation: Boolean = true,
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

    fun cameraMode(headingUp: Boolean = false): String =
        if (isFollowing) {
            MapLocationFollow.followingCameraMode(headingUp)
        } else {
            MapLocationFollow.FREE_CAMERA_MODE
        }

    /** North-up follow string — kept for older tests; prefer [cameraMode]. */
    val cameraMode: String
        get() = cameraMode(headingUp = false)

    val shouldMoveCameraWithLocation: Boolean
        get() = isFollowing &&
            hasLocationFix &&
            MapLocationFollow.cameraTracksLocationWhileFollowing
}
