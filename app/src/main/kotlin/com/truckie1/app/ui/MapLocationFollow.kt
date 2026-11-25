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
 * Recenter runs **one** smooth animate to lat/lng + default closer zoom, then
 * enters follow with a **zero-duration** LocationComponent transition so there is
 * no second zoom and no camera fight (ATNA-61).
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
     * Single Recenter camera path (ATNA-61):
     *
     * 1. Exit TRACKING (`CameraMode.NONE`)
     * 2. One [org.maplibre.android.maps.MapLibreMap.animateCamera] to lat/lng + default zoom
     * 3. Enter follow with **transition duration 0** and **no zoom argument**
     *
     * MapLibre's bare `setCameraMode(mode)` defaults to a **750ms** transition — that was
     * the #44 second zoom / off-centre after animate finished. Do not re-introduce sync
     * [moveCamera], `setCameraMode(..., zoom)`, or GPS `moveCamera` while following.
     */
    fun cameraPlanForRecenter(latitude: Double, longitude: Double): RecenterCameraPlan =
        RecenterCameraPlan(
            target = cameraTargetForRecenter(latitude, longitude),
            animationDurationMs = RECENTER_ANIMATION_MS,
            followEnterTransitionMs = FOLLOW_ENTER_TRANSITION_MS,
            applyZoomOnFollowEnter = false,
        )

    /**
     * Ordered camera ops for Recenter — one animate, then instant follow enter.
     * Used by tests to assert no second zoom / hard jump.
     */
    fun cameraOperations(plan: RecenterCameraPlan): List<RecenterCameraOp> = listOf(
        RecenterCameraOp.ExitTracking,
        RecenterCameraOp.AnimateToLatLngZoom(
            latitude = plan.target.latitude,
            longitude = plan.target.longitude,
            zoom = plan.target.zoom,
            durationMs = plan.animationDurationMs,
        ),
        RecenterCameraOp.EnterFollow(
            transitionMs = plan.followEnterTransitionMs,
            zoom = if (plan.applyZoomOnFollowEnter) plan.target.zoom else null,
        ),
    )

    /** True when the plan is exactly one animate then zero-duration follow (ATNA-61). */
    fun isSingleCameraPath(plan: RecenterCameraPlan): Boolean =
        plan.followEnterTransitionMs == 0L && !plan.applyZoomOnFollowEnter

    /**
     * Second zoom = LocationComponent follow-enter transition (>0ms) and/or passing zoom
     * into setCameraMode after animateCamera already set zoom (#44).
     */
    fun causesSecondZoom(plan: RecenterCameraPlan): Boolean =
        plan.followEnterTransitionMs > 0L || plan.applyZoomOnFollowEnter

    /** Hard jump = sync moveCamera / instant cameraPosition (ATNA-60 forbids). */
    fun usesHardCameraJump(plan: RecenterCameraPlan): Boolean = false

    /**
     * Steady follow: LocationComponent owns the camera. Do not drive moveCamera /
     * animateCamera on GPS ticks (that was a second path fighting Recenter).
     */
    fun shouldDriveCameraOnLocationFix(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): Boolean = false

    /**
     * Bootstrap follow mode once when Compose says FOLLOWING but LocationComponent
     * is still NONE (initial fix). Not a camera animate — transition duration 0.
     */
    fun shouldEnableFollowMode(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): Boolean =
        isFollowing && !recenterInFlight && !locationComponentAlreadyTracking

    /**
     * Heading-up SideEffect must not re-enter TRACKING mid-Recenter animation.
     */
    fun shouldSyncFollowingModeWithoutZoom(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
    ): Boolean = isFollowing && !recenterInFlight

    fun onLocationFixChanged(state: MapFollowState, hasLocationFix: Boolean): MapFollowState =
        state.copy(hasLocationFix = hasLocationFix)

    /** Duration of the single Recenter animateCamera. */
    const val RECENTER_ANIMATION_MS: Long = 750L

    /**
     * MapLibre bare `setCameraMode(mode)` uses 750ms — that re-animates after Recenter.
     * Enter follow with 0 so animateCamera is the only motion.
     */
    const val FOLLOW_ENTER_TRANSITION_MS: Long = 0L
}

/** Outcome of tapping Recenter: follow restored + camera at default closer zoom. */
data class RecenterResult(
    val followState: MapFollowState,
    val cameraTarget: MapCameraTarget,
    val resetsZoom: Boolean,
    val cameraPlan: RecenterCameraPlan,
)

/**
 * Single Recenter plan: one animateCamera, then enter follow without a second zoom.
 */
data class RecenterCameraPlan(
    val target: MapCameraTarget,
    val animationDurationMs: Long,
    /** Must be 0 — MapLibre default 750ms follow-enter was the #44 double zoom. */
    val followEnterTransitionMs: Long = MapLocationFollow.FOLLOW_ENTER_TRANSITION_MS,
    /** Must be false — zoom belongs only to animateCamera. */
    val applyZoomOnFollowEnter: Boolean = false,
)

/** Explicit ops for the one Recenter path (tests / docs). */
sealed class RecenterCameraOp {
    data object ExitTracking : RecenterCameraOp()

    data class AnimateToLatLngZoom(
        val latitude: Double,
        val longitude: Double,
        val zoom: Double,
        val durationMs: Long,
    ) : RecenterCameraOp()

    data class EnterFollow(
        val transitionMs: Long,
        val zoom: Double?,
    ) : RecenterCameraOp()
}

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
