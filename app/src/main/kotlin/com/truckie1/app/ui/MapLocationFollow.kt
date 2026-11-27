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
 * enters follow via a **bare** LocationComponent camera-mode set after syncing
 * last location (ATNA-61 + ATNA-66).
 *
 * Do **not** enter follow with `setCameraMode(mode, transitionMs=0, …)` — MapLibre's
 * LocationCameraController can leave `isTransitioning` stuck, so later
 * `forceLocationUpdate` moves the puck but not the camera (center-lock broken while
 * moving). After Recenter the camera is already framed; sync last location then use
 * the bare `cameraMode =` setter so `Utils.immediateAnimation` completes cleanly.
 *
 * Steady follow: LocationComponent TRACKING / TRACKING_GPS owns the camera via
 * forceLocationUpdate — no GPS moveCamera (keeps Recenter single-path).
 *
 * With [ff_map_heading_up][FlagKeys.MAP_HEADING_UP], follow uses course-up
 * (MapLibre `TRACKING_GPS`) so the map orients to the direction of travel.
 *
 * Host wiring goes through [MapFollowReconcile] only (ATNA-69) — one command path
 * for puck push, follow enter/exit, heading sync, and Recenter.
 *
 * Gherkin: features/map_first_home.feature, features/map_heading_up.feature,
 * features/map_center_lock_follow.feature, features/map_blue_dot_single_path.feature
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
     * Single Recenter camera path (ATNA-61 / ATNA-66):
     *
     * 1. Exit TRACKING (`CameraMode.NONE`)
     * 2. One [org.maplibre.android.maps.MapLibreMap.animateCamera] to lat/lng + default zoom
     * 3. Sync last location, then bare `cameraMode = TRACKING|TRACKING_GPS` (no zoom arg,
     *    no zero-ms timed overload)
     *
     * Do not re-introduce sync [moveCamera] for Recenter, `setCameraMode(..., zoom)`,
     * GPS `moveCamera` while following, or `setCameraMode(mode, 0, null…)`.
     */
    fun cameraPlanForRecenter(latitude: Double, longitude: Double): RecenterCameraPlan =
        RecenterCameraPlan(
            target = cameraTargetForRecenter(latitude, longitude),
            animationDurationMs = RECENTER_ANIMATION_MS,
            followEnter = followEnterAfterFramedRecenter(),
        )

    /** Safe follow enter after Recenter animate has framed lat/lng+zoom. */
    fun followEnterAfterFramedRecenter(): FollowEnterSpec = FollowEnterSpec.afterFramedRecenter()

    /**
     * Ordered camera ops for Recenter — one animate, then safe bare follow enter.
     * Used by tests to assert no second zoom / hard jump / stuck transition.
     */
    fun cameraOperations(plan: RecenterCameraPlan): List<RecenterCameraOp> = listOf(
        RecenterCameraOp.ExitTracking,
        RecenterCameraOp.AnimateToLatLngZoom(
            latitude = plan.target.latitude,
            longitude = plan.target.longitude,
            zoom = plan.target.zoom,
            durationMs = plan.animationDurationMs,
        ),
        RecenterCameraOp.EnterFollow(spec = plan.followEnter),
    )

    /** True when the plan is exactly one animate then safe bare follow enter (ATNA-61/66). */
    fun isSingleCameraPath(plan: RecenterCameraPlan): Boolean =
        plan.followEnter.usesBareCameraModeSetter &&
            !plan.followEnter.applyZoom &&
            !plan.followEnter.usesZeroDurationTimedTransitionOverload &&
            plan.followEnter.syncsLastLocationBeforeEnter

    /**
     * Second zoom = timed follow-enter transition (>0ms) and/or passing zoom
     * into setCameraMode after animateCamera already set zoom (#44).
     */
    fun causesSecondZoom(plan: RecenterCameraPlan): Boolean =
        plan.followEnter.applyZoom ||
            (plan.followEnter.timedTransitionMs != null && plan.followEnter.timedTransitionMs > 0L)

    /** Hard jump = sync moveCamera / instant cameraPosition (ATNA-60 forbids). */
    fun usesHardCameraJump(plan: RecenterCameraPlan): Boolean = false

    /**
     * Zero-ms timed `setCameraMode(mode, 0, …)` can leave MapLibre
     * LocationCameraController.isTransitioning stuck (ATNA-66).
     */
    fun causesFollowEnterTransitionStickRisk(enter: FollowEnterSpec): Boolean =
        enter.usesZeroDurationTimedTransitionOverload

    /** Follow enter is safe for subsequent forceLocationUpdate → camera tracking. */
    fun followEnterAllowsLocationCameraUpdates(enter: FollowEnterSpec): Boolean =
        !causesFollowEnterTransitionStickRisk(enter) &&
            (enter.usesBareCameraModeSetter || (enter.timedTransitionMs != null && enter.timedTransitionMs > 0L))

    /**
     * Steady follow while moving (ATNA-66): LocationComponent owns the camera.
     * Do not drive moveCamera / animateCamera on GPS ticks (that fought Recenter).
     */
    fun steadyFollowCameraAction(
        isFollowing: Boolean,
        hasLocationFix: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): SteadyFollowCameraAction {
        if (!isFollowing || !hasLocationFix || recenterInFlight) {
            return SteadyFollowCameraAction.None
        }
        return if (locationComponentAlreadyTracking) {
            SteadyFollowCameraAction.RelyOnTracking
        } else {
            SteadyFollowCameraAction.EnableTracking
        }
    }

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
     * is still NONE (initial fix). Uses bare camera-mode set (not 0ms timed overload).
     */
    fun shouldEnableFollowMode(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        locationComponentAlreadyTracking: Boolean,
    ): Boolean =
        steadyFollowCameraAction(
            isFollowing = isFollowing,
            hasLocationFix = true,
            recenterInFlight = recenterInFlight,
            locationComponentAlreadyTracking = locationComponentAlreadyTracking,
        ) == SteadyFollowCameraAction.EnableTracking

    /**
     * Heading-up may sync TRACKING↔TRACKING_GPS while following, but never mid-Recenter.
     * Prefer [MapFollowReconcile.needsModeSync] which also checks mode mismatch.
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
     * Legacy: ATNA-61 used a 0ms timed setCameraMode overload. That is **unsafe**
     * (ATNA-66 stick risk). Prefer [FollowEnterSpec.afterFramedRecenter].
     */
    @Deprecated("ATNA-66: use FollowEnterSpec.afterFramedRecenter() bare setter instead")
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
 * Single Recenter plan: one animateCamera, then safe bare follow enter.
 */
data class RecenterCameraPlan(
    val target: MapCameraTarget,
    val animationDurationMs: Long,
    val followEnter: FollowEnterSpec = FollowEnterSpec.afterFramedRecenter(),
) {
    /** Compatibility for older tests — bare enter has no timed transition. */
    val followEnterTransitionMs: Long
        get() = followEnter.timedTransitionMs ?: -1L

    val applyZoomOnFollowEnter: Boolean
        get() = followEnter.applyZoom
}

/**
 * How to enter LocationComponent follow after the camera is already framed
 * (Recenter animate finished) or on first bootstrap.
 *
 * ATNA-66: never use [timedTransitionMs] == 0 with the timed setCameraMode overload.
 */
data class FollowEnterSpec(
    val usesBareCameraModeSetter: Boolean,
    val syncsLastLocationBeforeEnter: Boolean,
    val applyZoom: Boolean,
    val timedTransitionMs: Long? = null,
) {
    val usesZeroDurationTimedTransitionOverload: Boolean
        get() = timedTransitionMs == 0L

    companion object {
        fun afterFramedRecenter(): FollowEnterSpec =
            FollowEnterSpec(
                usesBareCameraModeSetter = true,
                syncsLastLocationBeforeEnter = true,
                applyZoom = false,
                timedTransitionMs = null,
            )
    }
}

/** Steady-follow decision while the device moves (center lock). */
sealed class SteadyFollowCameraAction {
    /** Recenter in flight, not following, or no fix — do nothing. */
    data object None : SteadyFollowCameraAction()

    /** Compose FOLLOWING but LocationComponent not yet TRACKING — bare enable. */
    data object EnableTracking : SteadyFollowCameraAction()

    /** Already TRACKING / TRACKING_GPS — forceLocationUpdate only; no moveCamera. */
    data object RelyOnTracking : SteadyFollowCameraAction()
}

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
        val spec: FollowEnterSpec,
    ) : RecenterCameraOp() {
        @Deprecated("Use spec.timedTransitionMs")
        val transitionMs: Long
            get() = spec.timedTransitionMs ?: -1L

        @Deprecated("Use spec.zoomOrNull / applyZoom")
        val zoom: Double?
            get() = null
    }
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
