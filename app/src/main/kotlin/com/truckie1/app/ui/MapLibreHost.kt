package com.truckie1.app.ui

import android.graphics.Color
import android.location.Location
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.RouteStop
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap.CancelableCallback

/**
 * Thin MapLibre command executor (ATNA-69 / ATNA-90 / ATNA-117).
 *
 * Owns GeoJSON style overlays, LocationComponent, and [executeFollowCommands].
 * Compose feeds a [MapLibreBindSnapshot]; this type does **not** look up
 * [TruckieApp] feature flags during bind.
 *
 * Gherkin: features/map_home_orchestration.feature,
 * features/map_blue_dot_single_path.feature, features/map_style_overlays.feature
 */
internal class MapLibreHost(
    val mapView: MapView,
    private val context: android.content.Context,
) {
    private var mapRef: MapLibreMap? = null
    /** Course-up while following (`ff_map_heading_up`). */
    var headingUpEnabled: Boolean = false
    private var locationActivated: Boolean = false
    private var followGesturesBound: Boolean = false
    private var ignoreTrackingDismiss: Boolean = false
    private var styleReady: Boolean = false
    private var styleLoading: Boolean = false
    private val pendingStyleReady = mutableListOf<(Style) -> Unit>()
    /** Style instance overlays were last bound against (identity check). */
    private var boundStyle: Style? = null
    /** Street-label tweaks applied for the current [boundStyle] (`ff_map_street_labels`). */
    private var streetLabelPriorityApplied: Boolean = false
    var hasAppliedInitialCamera: Boolean = false
    /** Last fused fix pushed to LocationComponent (for follow-enter lastLocation sync). */
    private var lastFollowFix: Location? = null

    companion object {
        /** MapLibreMap must call [applyOverlays] — not a parallel inline bind sequence. */
        const val usesSingleApplyOverlaysPath: Boolean = true
        const val usesDuplicateOverlayBinds: Boolean = false
    }

    /**
     * Loads the basemap style once. Concurrent AndroidView updates coalesce into
     * a single setStyle; recompositions rebind GeoJSON overlays on the live style.
     * Rebinds when style identity drifts (orphaned overlay layers were the root
     * cause of invisible W5-8 markers after style reload races).
     */
    fun ensureStyle(map: MapLibreMap, onReady: (Style) -> Unit) {
        mapRef = map
        // OSM / OpenFreeMap: keep MapLibre attribution visible (ATNA-116).
        applyMapAttributionPolicy(map)
        val existing = map.style
        val fullyLoaded = existing != null && existing.isFullyLoaded
        val decision = MapLibreStyleLifecycle.decide(
            styleReady = styleReady,
            styleLoading = styleLoading,
            hasExistingFullyLoadedStyle = fullyLoaded,
            boundStyleMatchesExisting = boundStyle != null && boundStyle === existing,
        )
        when (decision.action) {
            StyleEnsureAction.REUSE_EXISTING -> {
                val style = existing ?: return
                if (decision.recreateOverlays) {
                    resetOverlays()
                    boundStyle = style
                }
                onReady(style)
            }
            StyleEnsureAction.ENQUEUE_WHILE_LOADING -> {
                pendingStyleReady.add(onReady)
            }
            StyleEnsureAction.START_LOAD -> {
                pendingStyleReady.add(onReady)
                styleLoading = true
                map.setStyle(Style.Builder().fromUri(MapCameraDefaults.STYLE_URI)) { style ->
                    resetOverlays()
                    boundStyle = style
                    streetLabelPriorityApplied = false
                    styleReady = true
                    styleLoading = false
                    val callbacks = pendingStyleReady.toList()
                    pendingStyleReady.clear()
                    callbacks.forEach { callback -> callback(style) }
                }
            }
        }
    }

    /**
     * Idempotent OpenFreeMap Liberty label priority (`ff_map_street_labels`).
     * Applied once per loaded style instance so recompositions do not thrash layers.
     */
    fun applyStreetLabelPriority(style: Style, enabled: Boolean) {
        if (!enabled) return
        if (streetLabelPriorityApplied && boundStyle === style) return
        MapStreetLabelPolicy.applyToStyle(style, enabled = true)
        streetLabelPriorityApplied = true
    }

    /**
     * Style layers die with the Style instance; clear LocationComponent so the
     * next bind reactivates against the live style. Overlay GeoJSON is rewritten
     * on every [applyOverlays] / bind call (idempotent ensure + setGeoJson).
     */
    private fun resetOverlays() {
        if (locationActivated) {
            runCatching {
                mapRef?.locationComponent?.isLocationComponentEnabled = false
                mapRef?.locationComponent?.onDestroy()
            }
            locationActivated = false
        }
        followGesturesBound = false
    }

    fun bindStops(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        stops: List<RouteStop>,
    ) {
        mapRef = map
        // Numbered style-image sprites for this stop list (MapLibre addImage atlas).
        MapStopMarkersCatalog.ensureStopMarkerImages(style, stops)
        MapStyleOverlayBind.bindStops(style, stops)
    }

    fun bindRoutePath(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        points: List<LatLngPoint>,
    ) {
        mapRef = map
        MapStyleOverlayBind.bindRoute(style, points)
    }

    fun bindBridges(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        bridges: List<BridgeSymbolSpec>,
    ) {
        mapRef = map
        BridgeMarkersCatalog.ensureBridgeMarkerImage(style, context)
        // ATNA-73 / ATNA-90 / ATNA-117: Compose already filtered + pre-labelled —
        // host does not look up TruckieApp flags or re-filter.
        MapStyleOverlayBind.bindBridges(style, bridges)
    }

    /**
     * **One** overlay bind path for home map (ATNA-117 / ATNA-121 / ATNA-122).
     *
     * Bind order: bridges → optional mid (LocationComponent puck) → route →
     * stops, then [MapStyleOverlayBind.reconcileOverlayZOrder] so MapLibre
     * native layer order is **stops top, route next, others below**.
     * Callers must not also invoke bindStops/bindRoute/bindBridges in parallel.
     */
    fun applyOverlays(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        snapshot: MapLibreBindSnapshot,
        afterBridgesBeforeRoute: (() -> Unit)? = null,
    ) {
        applyStreetLabelPriority(style, enabled = snapshot.prioritiseStreetLabels)
        bindBridges(
            mapView = mapView,
            map = map,
            style = style,
            bridges = snapshot.bridges,
        )
        // LocationComponent inserts its own layers; reconcile lifts Truckie
        // overlays above them while keeping stops > route > bridges.
        afterBridgesBeforeRoute?.invoke()
        bindRoutePath(
            mapView = mapView,
            map = map,
            style = style,
            points = snapshot.routePathPoints,
        )
        bindStops(
            mapView = mapView,
            map = map,
            style = style,
            stops = snapshot.stops,
        )
        MapStyleOverlayBind.reconcileOverlayZOrder(style)
    }

    /** @deprecated Use [applyOverlays] — single path, no Annotation dual bind. */
    @Deprecated(
        message = "Use applyOverlays",
        replaceWith = ReplaceWith(
            "applyOverlays(mapView, map, style, snapshot)",
            "com.truckie1.app.ui.MapLibreHost",
        ),
    )
    fun applyAnnotations(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        snapshot: MapLibreBindSnapshot,
    ) = applyOverlays(mapView, map, style, snapshot)

    fun ensureLocationComponent(map: MapLibreMap, style: Style) {
        mapRef = map
        if (locationActivated) return
        val blue = Color.parseColor(MapLocationPuck.PUCK_COLOR_HEX)
        val options = LocationComponentOptions.builder(context)
            .foregroundTintColor(blue)
            .backgroundTintColor(Color.WHITE)
            .accuracyColor(blue)
            .accuracyAlpha(0.15f)
            .enableStaleState(false)
            .pulseEnabled(MapLocationPuck.pulseEnabled)
            .pulseFadeEnabled(MapLocationPuck.pulseFadeEnabled)
            .pulseColor(blue)
            .pulseAlpha(MapLocationPuck.PULSE_ALPHA)
            .pulseMaxRadius(MapLocationPuck.PULSE_MAX_RADIUS)
            .pulseSingleDuration(MapLocationPuck.PULSE_SINGLE_DURATION_MS)
            .build()
        val activation = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(options)
            .useDefaultLocationEngine(false)
            .build()
        val component = map.locationComponent
        component.activateLocationComponent(activation)
        component.isLocationComponentEnabled = false
        component.cameraMode = CameraMode.NONE
        component.renderMode = MapLocationFollow.followingRenderModeInt(headingUpEnabled)
        locationActivated = true
    }

    fun bindFollowGestures(map: MapLibreMap, onUserPanAway: () -> Unit) {
        mapRef = map
        if (followGesturesBound) return
        map.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                setCameraFollowing(false)
                onUserPanAway()
            }
        }
        if (locationActivated) {
            map.locationComponent.addOnCameraTrackingChangedListener(
                object : OnCameraTrackingChangedListener {
                    override fun onCameraTrackingDismissed() {
                        if (ignoreTrackingDismiss) return
                        setCameraFollowing(false)
                        onUserPanAway()
                    }

                    override fun onCameraTrackingChanged(currentMode: Int) = Unit
                },
            )
        }
        followGesturesBound = true
    }

    /** Snapshot for [MapFollowReconcile] (ATNA-69 single path). */
    fun followRuntime(
        isFollowing: Boolean,
        hasPuckFix: Boolean,
        showLocationPuck: Boolean,
        locationFollowEnabled: Boolean,
        headingUp: Boolean,
    ): MapFollowRuntime =
        MapFollowRuntime(
            isFollowing = isFollowing,
            hasPuckFix = hasPuckFix,
            showLocationPuck = showLocationPuck,
            locationFollowEnabled = locationFollowEnabled,
            headingUp = headingUp,
            recenterInFlight = isRecenterInFlight,
            tracking = isLocationTracking,
            trackingIsHeadingUp = isTrackingHeadingUp,
        )

    /** Sole executor for follow / puck / Recenter / heading commands (ATNA-69). */
    fun executeFollowCommands(commands: List<MapFollowCommand>) {
        for (command in commands) {
            when (command) {
                is MapFollowCommand.PushPuck ->
                    updatePuck(command.fix, enabled = command.fix != null)
                is MapFollowCommand.EnterFollow -> {
                    headingUpEnabled = command.headingUp
                    setCameraFollowing(
                        following = true,
                        fromRecenterFinish = command.reason == FollowEnterReason.RecenterFinish,
                    )
                }
                MapFollowCommand.ExitFollow -> setCameraFollowing(false)
                is MapFollowCommand.SyncFollowModes -> syncFollowModes(command.headingUp)
                is MapFollowCommand.HardCamera -> applyCamera(command.target)
                is MapFollowCommand.RecenterAnimate -> applyRecenter(command.target)
                MapFollowCommand.ClearRecenterArm -> clearRecenterArm()
                MapFollowCommand.ArmRecenter -> Unit // forbidden — never emit from reconcile
            }
        }
    }

    private fun updatePuck(fix: LocationFix?, enabled: Boolean) {
        val map = mapRef ?: return
        if (!locationActivated) return
        val component = map.locationComponent
        if (!enabled || fix == null) {
            runCatching { component.isLocationComponentEnabled = false }
            return
        }
        runCatching {
            component.isLocationComponentEnabled = true
            val androidFix = fix.toAndroidLocation()
            lastFollowFix = androidFix
            component.forceLocationUpdate(androidFix)
        }
    }

    private fun applyCamera(target: MapCameraTarget) {
        val map = mapRef ?: return
        runCatching {
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(target.latitude, target.longitude))
                .zoom(target.zoom)
                .build()
        }
    }

    /** True while Recenter's single animateCamera is in flight (ATNA-61). */
    val isRecenterInFlight: Boolean
        get() = ignoreTrackingDismiss

    val isLocationTracking: Boolean
        get() {
            if (!locationActivated) return false
            val mode = mapRef?.locationComponent?.cameraMode ?: return false
            return mode == CameraMode.TRACKING || mode == CameraMode.TRACKING_GPS
        }

    val isTrackingHeadingUp: Boolean
        get() {
            if (!locationActivated) return false
            return mapRef?.locationComponent?.cameraMode == CameraMode.TRACKING_GPS
        }

    fun clearRecenterArm() {
        ignoreTrackingDismiss = false
    }

    /**
     * Apply TRACKING↔TRACKING_GPS + render mode while already following (ATNA-69).
     * Does not exit follow or touch zoom — orientation lock stays isolated from Recenter.
     */
    private fun syncFollowModes(headingUp: Boolean) {
        if (!locationActivated) return
        if (ignoreTrackingDismiss) return
        headingUpEnabled = headingUp
        val map = mapRef ?: return
        runCatching {
            val component = map.locationComponent
            component.renderMode = MapLocationFollow.followingRenderModeInt(headingUp)
            component.cameraMode = MapLocationFollow.followingCameraModeInt(headingUp)
        }
    }

    /**
     * Single Recenter path (ATNA-61 / ATNA-66): exit TRACKING → one
     * animateCamera(latLng+zoom) → sync last location → bare cameraMode follow enter
     * (no second zoom, no 0ms timed overload stick risk). Arms dismiss-ignore only here
     * (not via Compose SideEffect — that left the arm stuck when Recenter could not run).
     */
    private fun applyRecenter(target: MapCameraTarget) {
        val map = mapRef ?: return
        val plan = MapLocationFollow.cameraPlanForRecenter(target.latitude, target.longitude)
        val latLng = LatLng(plan.target.latitude, plan.target.longitude)
        ignoreTrackingDismiss = true
        runCatching {
            if (locationActivated) {
                map.locationComponent.renderMode =
                    MapLocationFollow.followingRenderModeInt(headingUpEnabled)
                map.locationComponent.cameraMode = CameraMode.NONE
            }
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, plan.target.zoom),
                plan.animationDurationMs.toInt(),
                object : CancelableCallback {
                    override fun onFinish() {
                        enterFollowAfterRecenter(plan.target)
                    }

                    override fun onCancel() {
                        // Land on target zoom even if cancelled, still one path.
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(latLng, plan.target.zoom),
                        )
                        enterFollowAfterRecenter(plan.target)
                    }
                },
            )
        }.onFailure {
            ignoreTrackingDismiss = false
        }
    }

    private fun enterFollowAfterRecenter(framed: MapCameraTarget) {
        val enter = MapLocationFollow.followEnterAfterFramedRecenter()
        if (enter.syncsLastLocationBeforeEnter) {
            syncLastLocationToFramedTarget(framed)
        }
        setCameraFollowing(
            following = true,
            fromRecenterFinish = true,
        )
        ignoreTrackingDismiss = false
    }

    /**
     * Push framed Recenter lat/lng into LocationComponent before bare follow enter so
     * MapLibre `Utils.immediateAnimation` sees matching lastLocation (ATNA-66).
     */
    private fun syncLastLocationToFramedTarget(framed: MapCameraTarget) {
        if (!locationActivated) return
        val map = mapRef ?: return
        runCatching {
            val loc = (lastFollowFix?.let { Location(it) } ?: Location("recenter")).apply {
                latitude = framed.latitude
                longitude = framed.longitude
            }
            lastFollowFix = loc
            map.locationComponent.forceLocationUpdate(loc)
        }
    }

    /**
     * Enable/disable LocationComponent follow.
     *
     * Uses a **bare** `cameraMode = TRACKING|TRACKING_GPS` set only (ATNA-66 / ATNA-69).
     * After Recenter animate + lastLocation sync, MapLibre takes the immediateAnimation
     * path — no second zoom. Timed `setCameraMode` overload removed (stick risk).
     */
    private fun setCameraFollowing(
        following: Boolean,
        fromRecenterFinish: Boolean = false,
    ) {
        if (!locationActivated) return
        if (!MapFollowReconcile.mayEnterFollow(
                recenterInFlight = ignoreTrackingDismiss,
                fromRecenterFinish = fromRecenterFinish,
            ) && following
        ) {
            return
        }
        val map = mapRef ?: return
        runCatching {
            val component = map.locationComponent
            if (following) {
                component.renderMode =
                    MapLocationFollow.followingRenderModeInt(headingUpEnabled)
                // Bare setter only — no timed overload, no zoom arg.
                component.cameraMode =
                    MapLocationFollow.followingCameraModeInt(headingUpEnabled)
            } else {
                component.cameraMode = CameraMode.NONE
            }
        }
    }

    fun onStart() {
        mapRef?.locationComponent?.takeIf { locationActivated }?.onStart()
    }

    fun onStop() {
        mapRef?.locationComponent?.takeIf { locationActivated }?.onStop()
    }

    fun dispose() {
        resetOverlays()
        styleReady = false
        styleLoading = false
        pendingStyleReady.clear()
        boundStyle = null
        hasAppliedInitialCamera = false
        mapRef = null
    }

    /**
     * OpenStreetMap / OpenFreeMap attribution must stay visible to drivers
     * (ATNA-116 / ODbL). Never strip MapLibre attribution for chrome aesthetics.
     */
    fun applyMapAttributionPolicy(map: MapLibreMap) {
        if (!LicensesCatalog.mapAttributionRemainsEnabled) return
        runCatching {
            map.uiSettings.isAttributionEnabled = true
        }
    }

}

private fun LocationFix.toAndroidLocation(): Location =
    Location("fused").also { loc ->
        loc.latitude = latitude
        loc.longitude = longitude
        accuracyMeters?.let { loc.accuracy = it }
        bearingDegrees?.let { loc.bearing = it }
        speedMetersPerSecond?.let { loc.speed = it }
    }
