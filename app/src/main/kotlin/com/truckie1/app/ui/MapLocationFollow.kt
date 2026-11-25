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
     * MapLibre apply plan for Recenter.
     *
     * Prefer [RecenterZoomApplyMode.ZOOM_WHILE_TRACKING]: enable follow, then call
     * LocationComponent.zoomWhileTracking(defaultZoom) — the MapLibre API for changing
     * zoom under TRACKING / TRACKING_GPS (ATNA-59 follow-up). setCameraMode(..., zoom)
     * alone is cancelled when a subsequent bare setCameraFollowing / GPS tick re-enters
     * TRACKING without zoom.
     */
    fun cameraPlanForRecenter(latitude: Double, longitude: Double): RecenterCameraPlan =
        RecenterCameraPlan(
            target = cameraTargetForRecenter(latitude, longitude),
            zoomApplyMode = RecenterZoomApplyMode.ZOOM_WHILE_TRACKING,
            animationDurationMs = RECENTER_ANIMATION_MS,
            exitTrackingBeforeApply = true,
            useZoomWhileTracking = true,
        )

    /**
     * Models MapLibre Recenter zoom outcome for tests.
     *
     * - [RecenterZoomApplyMode.ANIMATE_CAMERA_LAT_LNG_ZOOM] + immediate TRACKING → priorZoom (#24)
     * - [RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM] cancelled by a later bare
     *   setCameraFollowing → priorZoom unless [gpsTickCancelsTransition] is false
     * - [RecenterZoomApplyMode.ZOOM_WHILE_TRACKING] → default closer zoom even after GPS ticks
     */
    fun effectiveZoomAfterRecenter(
        plan: RecenterCameraPlan,
        priorZoom: Double,
        trackingEnabledImmediately: Boolean = true,
        trackingAlreadyActiveWithoutZoom: Boolean = false,
        gpsTickCancelsTransition: Boolean = false,
    ): Double = when (plan.zoomApplyMode) {
        RecenterZoomApplyMode.ANIMATE_CAMERA_LAT_LNG_ZOOM ->
            if (trackingEnabledImmediately) priorZoom else plan.target.zoom
        RecenterZoomApplyMode.TRACKING_TRANSITION_WITH_ZOOM -> {
            when {
                gpsTickCancelsTransition -> priorZoom
                trackingAlreadyActiveWithoutZoom && !plan.exitTrackingBeforeApply -> priorZoom
                else -> plan.target.zoom
            }
        }
        RecenterZoomApplyMode.ZOOM_WHILE_TRACKING -> plan.target.zoom
    }

    /**
     * Heading-up SideEffect / per-fix camera sync must not call bare setCameraFollowing
     * during Recenter — that cancels zoomWhileTracking / setCameraMode zoom (ATNA-59).
     */
    fun shouldSyncFollowingModeWithoutZoom(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
    ): Boolean = isFollowing && !recenterInFlight

    /**
     * While already following, LocationComponent TRACKING owns the camera — do not
     * re-apply camera on every fused fix (that stomps Recenter zoom).
     */
    fun shouldApplyFollowCameraOnLocationFix(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): Boolean {
        if (recenterInFlight) return false
        if (!isFollowing) return false
        // First transition into follow still needs a camera apply; steady tracking does not.
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
     * setCameraMode(..., zoom) transition. Still fragile if a later bare
     * setCameraFollowing cancels the transition mid-flight (ATNA-59 GPS race).
     */
    TRACKING_TRANSITION_WITH_ZOOM,

    /**
     * Preferred (ATNA-59): enable TRACKING then
     * [org.maplibre.android.location.LocationComponent.zoomWhileTracking].
     */
    ZOOM_WHILE_TRACKING,
}

/** Camera ops Recenter must perform so MapLibre honors [MapCameraDefaults.ZOOM]. */
data class RecenterCameraPlan(
    val target: MapCameraTarget,
    val zoomApplyMode: RecenterZoomApplyMode,
    val animationDurationMs: Long,
    /**
     * When true, drop to [org.maplibre.android.location.modes.CameraMode.NONE] before
     * re-enabling TRACKING so mode changes are not no-ops (ATNA-59).
     */
    val exitTrackingBeforeApply: Boolean = true,
    /** When true, host must call LocationComponent.zoomWhileTracking(defaultZoom). */
    val useZoomWhileTracking: Boolean = true,
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
