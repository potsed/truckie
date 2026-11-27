package com.truckie1.app.ui

import com.truckie1.maps.MapCameraTarget

/**
 * Single follow / puck / Recenter / heading-up decision path (ATNA-69).
 *
 * Every MapLibre host event maps to one ordered [MapFollowCommand] list.
 * The host executes those commands only — no parallel SideEffect + GPS +
 * AndroidView TRACKING enter paths, no separate SideEffect Recenter arm.
 *
 * Gherkin: features/map_blue_dot_single_path.feature
 */
object MapFollowReconcile {
    /** Catalog: one path owns puck push and follow camera policy. */
    const val singlePathOwnsPuckAndFollow: Boolean = true

    /** Follow enter always uses bare cameraMode setter (never timed overload). */
    const val usesBareFollowEnterOnly: Boolean = true

    fun needsModeSync(
        isFollowing: Boolean,
        recenterInFlight: Boolean,
        tracking: Boolean,
        headingUpDesired: Boolean,
        trackingIsHeadingUp: Boolean,
    ): Boolean =
        isFollowing &&
            !recenterInFlight &&
            tracking &&
            headingUpDesired != trackingIsHeadingUp

    fun mayEnterFollow(
        recenterInFlight: Boolean,
        fromRecenterFinish: Boolean,
    ): Boolean = fromRecenterFinish || !recenterInFlight

    fun reconcile(
        event: MapFollowEvent,
        runtime: MapFollowRuntime,
    ): List<MapFollowCommand> =
        when (event) {
            is MapFollowEvent.LocationUpdated -> onLocationUpdated(event, runtime)
            is MapFollowEvent.HeadingChanged -> onHeadingChanged(event, runtime)
            MapFollowEvent.ComposeSync -> onComposeSync(runtime)
            MapFollowEvent.UserPan -> listOf(MapFollowCommand.ExitFollow)
            MapFollowEvent.FollowBecameFree ->
                if (runtime.tracking) listOf(MapFollowCommand.ExitFollow) else emptyList()
            is MapFollowEvent.RecenterRequested -> onRecenterRequested(event)
            MapFollowEvent.RecenterUnavailable ->
                if (runtime.recenterInFlight) {
                    listOf(MapFollowCommand.ClearRecenterArm)
                } else {
                    emptyList()
                }
        }

    private fun onLocationUpdated(
        event: MapFollowEvent.LocationUpdated,
        runtime: MapFollowRuntime,
    ): List<MapFollowCommand> {
        if (!runtime.showLocationPuck) {
            return listOf(
                MapFollowCommand.PushPuck(null),
                MapFollowCommand.HardCamera(event.camera),
            )
        }
        val commands = mutableListOf<MapFollowCommand>(MapFollowCommand.PushPuck(event.fix))
        val followActive = runtime.locationFollowEnabled && runtime.showLocationPuck
        if (!followActive) {
            commands += MapFollowCommand.HardCamera(event.camera)
            return commands
        }
        when (
            MapLocationFollow.steadyFollowCameraAction(
                isFollowing = runtime.isFollowing,
                hasLocationFix = event.fix != null,
                recenterInFlight = runtime.recenterInFlight,
                locationComponentAlreadyTracking = runtime.tracking,
            )
        ) {
            SteadyFollowCameraAction.EnableTracking ->
                commands += MapFollowCommand.EnterFollow(
                    headingUp = runtime.headingUp,
                    reason = FollowEnterReason.Bootstrap,
                )
            SteadyFollowCameraAction.RelyOnTracking -> {
                if (
                    needsModeSync(
                        isFollowing = runtime.isFollowing,
                        recenterInFlight = runtime.recenterInFlight,
                        tracking = runtime.tracking,
                        headingUpDesired = runtime.headingUp,
                        trackingIsHeadingUp = runtime.trackingIsHeadingUp,
                    )
                ) {
                    commands += MapFollowCommand.SyncFollowModes(headingUp = runtime.headingUp)
                }
            }
            SteadyFollowCameraAction.None -> {
                if (runtime.isFollowing && event.fix == null) {
                    commands += MapFollowCommand.HardCamera(event.camera)
                }
            }
        }
        return commands
    }

    private fun onHeadingChanged(
        event: MapFollowEvent.HeadingChanged,
        runtime: MapFollowRuntime,
    ): List<MapFollowCommand> {
        if (
            !needsModeSync(
                isFollowing = runtime.isFollowing,
                recenterInFlight = runtime.recenterInFlight,
                tracking = runtime.tracking,
                headingUpDesired = event.headingUp,
                trackingIsHeadingUp = runtime.trackingIsHeadingUp,
            )
        ) {
            return emptyList()
        }
        return listOf(MapFollowCommand.SyncFollowModes(headingUp = event.headingUp))
    }

    private fun onComposeSync(runtime: MapFollowRuntime): List<MapFollowCommand> {
        // Do not guess a "stuck" arm here — live Recenter also has tracking=false.
        // Stuck arms clear only via RecenterUnavailable (Recenter could not run).
        if (
            runtime.locationFollowEnabled &&
            runtime.showLocationPuck &&
            MapLocationFollow.shouldEnableFollowMode(
                isFollowing = runtime.isFollowing,
                recenterInFlight = runtime.recenterInFlight,
                locationComponentAlreadyTracking = runtime.tracking,
            )
        ) {
            return listOf(
                MapFollowCommand.EnterFollow(
                    headingUp = runtime.headingUp,
                    reason = FollowEnterReason.Bootstrap,
                ),
            )
        }
        if (
            runtime.locationFollowEnabled &&
            !runtime.isFollowing &&
            runtime.tracking
        ) {
            return listOf(MapFollowCommand.ExitFollow)
        }
        if (
            needsModeSync(
                isFollowing = runtime.isFollowing,
                recenterInFlight = runtime.recenterInFlight,
                tracking = runtime.tracking,
                headingUpDesired = runtime.headingUp,
                trackingIsHeadingUp = runtime.trackingIsHeadingUp,
            )
        ) {
            return listOf(MapFollowCommand.SyncFollowModes(headingUp = runtime.headingUp))
        }
        return emptyList()
    }

    private fun onRecenterRequested(
        event: MapFollowEvent.RecenterRequested,
    ): List<MapFollowCommand> {
        val plan = MapLocationFollow.cameraPlanForRecenter(event.latitude, event.longitude)
        return listOf(MapFollowCommand.RecenterAnimate(target = plan.target, plan = plan))
    }
}

/** Snapshot of Compose + LocationComponent state for one reconcile. */
data class MapFollowRuntime(
    val isFollowing: Boolean,
    val hasPuckFix: Boolean,
    val showLocationPuck: Boolean,
    val locationFollowEnabled: Boolean,
    val headingUp: Boolean,
    val recenterInFlight: Boolean,
    val tracking: Boolean,
    /** True when LocationComponent camera mode is TRACKING_GPS. */
    val trackingIsHeadingUp: Boolean,
)

/** Why the host should touch follow / puck / camera. */
sealed class MapFollowEvent {
    data class LocationUpdated(
        val fix: LocationFix?,
        val camera: MapCameraTarget,
    ) : MapFollowEvent()

    data class HeadingChanged(val headingUp: Boolean) : MapFollowEvent()

    /** Style ready / AndroidView update / SideEffect — one bootstrap sync. */
    data object ComposeSync : MapFollowEvent()

    data object UserPan : MapFollowEvent()

    data object FollowBecameFree : MapFollowEvent()

    data class RecenterRequested(
        val latitude: Double,
        val longitude: Double,
    ) : MapFollowEvent()

    /** Recenter token fired but no fix / flags off — clear stuck arm. */
    data object RecenterUnavailable : MapFollowEvent()
}

enum class FollowEnterReason {
    Bootstrap,
    RecenterFinish,
}

/** Imperative host ops produced by [MapFollowReconcile] (single path). */
sealed class MapFollowCommand {
    data class PushPuck(val fix: LocationFix?) : MapFollowCommand()

    data class EnterFollow(
        val headingUp: Boolean,
        val reason: FollowEnterReason,
    ) : MapFollowCommand()

    data object ExitFollow : MapFollowCommand()

    /** Apply TRACKING↔TRACKING_GPS + render mode while already following. */
    data class SyncFollowModes(val headingUp: Boolean) : MapFollowCommand()

    data class HardCamera(val target: MapCameraTarget) : MapFollowCommand()

    data class RecenterAnimate(
        val target: MapCameraTarget,
        val plan: RecenterCameraPlan,
    ) : MapFollowCommand()

    data object ClearRecenterArm : MapFollowCommand()

    /** Never emitted by reconcile — SideEffect arming is forbidden. */
    data object ArmRecenter : MapFollowCommand()
}
