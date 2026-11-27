package com.truckie1.app.ui

import android.graphics.Color
import android.location.Location
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truckie1.app.TruckieApp
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.RouteStop
import org.maplibre.android.MapLibre
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
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

/**
 * Full-bleed MapLibre map. Camera prefers the driver's **current location** via
 * Play Services Fused Location Provider (runtime permission + Settings resolution);
 * falls back to Brisbane when unavailable.
 *
 * When [showLocationPuck] is true (`ff_map_location_puck`), a standard blue
 * LocationComponent puck is shown at the same fused fix the camera uses.
 *
 * When [locationFollowEnabled] is true, follow mode keeps the puck centred and
 * LocationComponent tracks the camera until the driver pans away. Recenter
 * (owned by the parent overlay) runs **one** animateCamera to lat/lng + default
 * closer zoom, then enters follow via a bare camera-mode set after syncing last
 * location (ATNA-61 / ATNA-66 — not the 0ms timed setCameraMode overload).
 *
 * ATNA-69: every puck / follow / Recenter / heading-up change goes through
 * [MapFollowReconcile] → [MapLibreHost.executeFollowCommands] — one path
 * (no competing SideEffect + GPS + AndroidView TRACKING enter paths).
 *
 * When [headingUpEnabled] is true (`ff_map_heading_up`), follow uses
 * [CameraMode.TRACKING_GPS] so the map orients to the direction of travel.
 *
 * Stop markers use the Annotation plugin [SymbolManager] (not deprecated MarkerOptions).
 * When [showBridgeMarkers] is true (`ff_bridge_markers`), a separate SymbolManager
 * draws Australia-first bridge fixtures with optional clearance labels.
 *
 * When [prioritiseStreetLabels] is true (`ff_map_street_labels`), runtime
 * OpenFreeMap Liberty layer tweaks prefer highway names over bus-stop / POI clutter.
 *
 * When the native MapView cannot be created (e.g. Robolectric unit tests),
 * a high-contrast placeholder keeps the Compose tree testable.
 */
@Composable
fun MapLibreMap(
    stops: List<RouteStop>,
    modifier: Modifier = Modifier,
    locationProvider: CurrentLocationProvider? = null,
    settingsEnsurer: LocationSettingsEnsurer? = null,
    showLocationPuck: Boolean = false,
    locationFollowEnabled: Boolean = false,
    /** Course-up while following (`ff_map_heading_up`). */
    headingUpEnabled: Boolean = false,
    followState: MapFollowState = MapLocationFollow.initial(),
    onFollowStateChange: (MapFollowState) -> Unit = {},
    recenterToken: Int = 0,
    /** GraphHopper path points drawn on this same map (Navigate). */
    routePathPoints: List<LatLngPoint> = emptyList(),
    bridges: List<BridgePoint> = emptyList(),
    showBridgeMarkers: Boolean = false,
    /** Prefer street names over POI clutter (`ff_map_street_labels`). */
    prioritiseStreetLabels: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provider = locationProvider ?: remember { FusedCurrentLocationProvider(context) }
    val ensurer = settingsEnsurer ?: remember { PlayServicesLocationSettingsEnsurer(context) }
    val hostHolder = remember { mutableStateOf<MapLibreHost?>(null) }
    val followStateRef = remember { mutableStateOf(followState) }
    var lastHeadingUp by remember { mutableStateOf(headingUpEnabled) }
    SideEffect { followStateRef.value = followState }

    var cameraTarget by remember { mutableStateOf(MapCameraTarget.fallback()) }
    var locationFix by remember { mutableStateOf<LocationFix?>(null) }
    var hasPermission by remember {
        mutableStateOf(FusedCurrentLocationProvider.hasLocationPermission(context))
    }

    fun publishFollow(next: MapFollowState) {
        if (next != followStateRef.value) {
            followStateRef.value = next
            onFollowStateChange(next)
        }
    }

    fun runtimeFor(host: MapLibreHost, following: Boolean = followStateRef.value.isFollowing) =
        host.followRuntime(
            isFollowing = following,
            hasPuckFix = locationFix != null,
            showLocationPuck = showLocationPuck,
            locationFollowEnabled = locationFollowEnabled,
            headingUp = headingUpEnabled,
        )

    fun executeReconcile(host: MapLibreHost, event: MapFollowEvent, following: Boolean = followStateRef.value.isFollowing) {
        host.headingUpEnabled = headingUpEnabled
        host.executeFollowCommands(
            MapFollowReconcile.reconcile(event, runtimeFor(host, following)),
        )
    }

    // One SideEffect path: heading sync + compose bootstrap (never arms Recenter).
    SideEffect {
        val host = hostHolder.value ?: return@SideEffect
        if (lastHeadingUp != headingUpEnabled) {
            lastHeadingUp = headingUpEnabled
            executeReconcile(host, MapFollowEvent.HeadingChanged(headingUpEnabled))
        } else {
            executeReconcile(host, MapFollowEvent.ComposeSync)
        }
    }

    fun onLocationReady(fix: LocationFix?) {
        val permitted = FusedCurrentLocationProvider.hasLocationPermission(context)
        hasPermission = permitted
        if (showLocationPuck) {
            val resolved = MapLocationPuck.resolve(fix = fix, hasLocationPermission = permitted)
            cameraTarget = resolved.camera
            locationFix = resolved.puck
            if (locationFollowEnabled) {
                val withFix = MapLocationFollow.onLocationFixChanged(
                    followStateRef.value,
                    hasLocationFix = resolved.puck != null,
                )
                publishFollow(withFix)
            }
            hostHolder.value?.let { host ->
                executeReconcile(
                    host,
                    MapFollowEvent.LocationUpdated(
                        fix = resolved.puck,
                        camera = resolved.camera,
                    ),
                    following = followStateRef.value.isFollowing,
                )
            }
        } else {
            cameraTarget = MapCameraTarget.resolve(fix?.latitude, fix?.longitude)
            locationFix = null
            hostHolder.value?.let { host ->
                executeReconcile(
                    host,
                    MapFollowEvent.LocationUpdated(fix = null, camera = cameraTarget),
                    following = false,
                )
            }
        }
    }

    fun fetchAfterSettingsReady() {
        provider.fetchCurrent { fix ->
            onLocationReady(fix)
            if (showLocationPuck && FusedCurrentLocationProvider.hasLocationPermission(context)) {
                provider.startUpdates { update -> onLocationReady(update) }
            }
        }
    }

    val settingsResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (locationSettingsResolutionSucceeded(result.resultCode)) {
            fetchAfterSettingsReady()
        } else {
            onLocationReady(null)
        }
    }

    fun ensureSettingsThenFetch() {
        ensurer.ensureSettingsSatisfied(
            onReady = { fetchAfterSettingsReady() },
            onUnavailable = { onLocationReady(null) },
            onResolutionRequired = { request -> settingsResolutionLauncher.launch(request) },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasPermission = grants.values.any { it }
        if (hasPermission) {
            ensureSettingsThenFetch()
        } else {
            onLocationReady(null)
        }
    }

    LaunchedEffect(provider, ensurer, showLocationPuck) {
        if (FusedCurrentLocationProvider.hasLocationPermission(context)) {
            hasPermission = true
            ensureSettingsThenFetch()
        } else {
            permissionLauncher.launch(MapHomeCatalog.locationPermissions.toTypedArray())
        }
    }

    // One-shot Recenter via reconcile (ATNA-61/69). Unavailable → clear stuck arm.
    LaunchedEffect(recenterToken) {
        if (recenterToken <= 0) return@LaunchedEffect
        val host = hostHolder.value
        if (host == null || !locationFollowEnabled || !showLocationPuck) {
            host?.let { executeReconcile(it, MapFollowEvent.RecenterUnavailable) }
            return@LaunchedEffect
        }
        val fix = locationFix
        if (fix == null) {
            executeReconcile(host, MapFollowEvent.RecenterUnavailable)
            return@LaunchedEffect
        }
        val target = MapLocationFollow.cameraTargetForRecenter(fix.latitude, fix.longitude)
        cameraTarget = target
        executeReconcile(
            host,
            MapFollowEvent.RecenterRequested(
                latitude = fix.latitude,
                longitude = fix.longitude,
            ),
        )
    }

    LaunchedEffect(followState.isFollowing, locationFollowEnabled) {
        if (!locationFollowEnabled || !showLocationPuck) return@LaunchedEffect
        if (!followState.isFollowing) {
            hostHolder.value?.let { executeReconcile(it, MapFollowEvent.FollowBecameFree) }
        }
    }

    DisposableEffect(provider) {
        onDispose { provider.stopUpdates() }
    }

    val mapHost = remember {
        runCatching {
            MapLibre.getInstance(context.applicationContext)
            val view = MapView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                onCreate(null)
            }
            MapLibreHost(mapView = view, context = context)
        }.getOrNull()
    }
    hostHolder.value = mapHost

    if (mapHost == null) {
        val label = when {
            locationFix != null && showLocationPuck && followState.isFollowing ->
                "MapLibre · following location"
            locationFix != null && showLocationPuck -> "MapLibre · location puck"
            cameraTarget.usedFallback -> "MapLibre · Brisbane fallback"
            else -> "MapLibre · current location"
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("map_libre_surface"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        return
    }

    val mapView = mapHost.mapView

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    mapHost.onStart()
                }
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {
                    mapHost.onStop()
                    mapView.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mapHost.dispose()
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapHost.dispose()
                mapView.onDestroy()
            } catch (_: Throwable) {
                // Already destroyed or unavailable in test hosts.
            }
            if (hostHolder.value === mapHost) {
                hostHolder.value = null
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxSize()
            .testTag("map_libre_surface"),
        update = { view ->
            view.getMapAsync { map ->
                // Load style once. Re-calling setStyle on every recomposition orphans
                // SymbolManagers / LineManager / LocationComponent bound to the old style
                // (root cause of invisible bridge markers after location updates).
                mapHost.ensureStyle(map) { style ->
                    // ATNA-78: runtime Liberty layer tweaks (same style URL).
                    mapHost.applyStreetLabelPriority(
                        style = style,
                        enabled = prioritiseStreetLabels,
                    )
                    if (!mapHost.hasAppliedInitialCamera) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(cameraTarget.latitude, cameraTarget.longitude))
                            .zoom(cameraTarget.zoom)
                            .build()
                        mapHost.hasAppliedInitialCamera = true
                    }
                    mapHost.bindStops(mapView = view, map = map, style = style, stops = stops)
                    mapHost.bindRoutePath(mapView = view, map = map, style = style, points = routePathPoints)
                    if (showLocationPuck) {
                        mapHost.headingUpEnabled = headingUpEnabled
                        mapHost.ensureLocationComponent(map = map, style = style)
                        if (locationFollowEnabled) {
                            mapHost.bindFollowGestures(
                                map = map,
                                onUserPanAway = {
                                    mapHost.executeFollowCommands(
                                        MapFollowReconcile.reconcile(
                                            MapFollowEvent.UserPan,
                                            mapHost.followRuntime(
                                                isFollowing = followStateRef.value.isFollowing,
                                                hasPuckFix = locationFix != null,
                                                showLocationPuck = true,
                                                locationFollowEnabled = true,
                                                headingUp = headingUpEnabled,
                                            ),
                                        ),
                                    )
                                    publishFollow(
                                        MapLocationFollow.onUserPan(followStateRef.value),
                                    )
                                },
                            )
                        }
                        // Single path: puck push + follow bootstrap (no parallel enable).
                        mapHost.executeFollowCommands(
                            MapFollowReconcile.reconcile(
                                MapFollowEvent.LocationUpdated(
                                    fix = locationFix,
                                    camera = cameraTarget,
                                ),
                                mapHost.followRuntime(
                                    isFollowing = followStateRef.value.isFollowing,
                                    hasPuckFix = locationFix != null,
                                    showLocationPuck = true,
                                    locationFollowEnabled = locationFollowEnabled,
                                    headingUp = headingUpEnabled,
                                ),
                            ),
                        )
                        mapHost.executeFollowCommands(
                            MapFollowReconcile.reconcile(
                                MapFollowEvent.ComposeSync,
                                mapHost.followRuntime(
                                    isFollowing = followStateRef.value.isFollowing,
                                    hasPuckFix = locationFix != null,
                                    showLocationPuck = true,
                                    locationFollowEnabled = locationFollowEnabled,
                                    headingUp = headingUpEnabled,
                                ),
                            ),
                        )
                    } else {
                        mapHost.executeFollowCommands(
                            listOf(MapFollowCommand.PushPuck(null)),
                        )
                    }
                    // Bridges after LocationComponent so the W5-8 SymbolLayer sits above
                    // puck/accuracy layers and is never buried under the basemap stack.
                    mapHost.bindBridges(
                        mapView = view,
                        map = map,
                        style = style,
                        bridges = if (showBridgeMarkers) bridges else emptyList(),
                    )
                }
            }
        },
    )
}

/**
 * Holds stop + bridge [SymbolManager]s, Navigate [LineManager], and MapLibre
 * [org.maplibre.android.location.LocationComponent] across Compose recompositions.
 */
private class MapLibreHost(
    val mapView: MapView,
    private val context: android.content.Context,
) {
    private var symbolManager: SymbolManager? = null
    private var lineManager: LineManager? = null
    private var bridgeSymbolManager: SymbolManager? = null
    private var mapRef: MapLibreMap? = null
    /** Course-up while following (`ff_map_heading_up`). */
    var headingUpEnabled: Boolean = false
    private var locationActivated: Boolean = false
    private var followGesturesBound: Boolean = false
    private var ignoreTrackingDismiss: Boolean = false
    private var styleReady: Boolean = false
    private var styleLoading: Boolean = false
    private val pendingStyleReady = mutableListOf<(Style) -> Unit>()
    /** Style instance managers were last created against (identity check). */
    private var boundStyle: Style? = null
    /** Street-label tweaks applied for the current [boundStyle] (`ff_map_street_labels`). */
    private var streetLabelPriorityApplied: Boolean = false
    var hasAppliedInitialCamera: Boolean = false
    /** Last fused fix pushed to LocationComponent (for follow-enter lastLocation sync). */
    private var lastFollowFix: Location? = null

    /**
     * Loads the basemap style once. Concurrent AndroidView updates coalesce into
     * a single setStyle; recompositions rebind annotations on the live style.
     * Recreates managers when style identity drifts (orphaned SymbolManagers were
     * the root cause of invisible W5-8 markers after Jump to sample bridges).
     */
    fun ensureStyle(map: MapLibreMap, onReady: (Style) -> Unit) {
        mapRef = map
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
                if (decision.recreateManagers) {
                    resetAnnotationManagers()
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
                    resetAnnotationManagers()
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

    private fun resetAnnotationManagers() {
        runCatching { symbolManager?.onDestroy() }
        symbolManager = null
        runCatching { lineManager?.onDestroy() }
        lineManager = null
        runCatching { bridgeSymbolManager?.onDestroy() }
        bridgeSymbolManager = null
        if (locationActivated) {
            runCatching {
                mapRef?.locationComponent?.isLocationComponentEnabled = false
                mapRef?.locationComponent?.onDestroy()
            }
            locationActivated = false
        }
        followGesturesBound = false
    }

    private fun dropManagerIfOrphaned(
        manager: SymbolManager?,
        style: Style,
        clear: () -> Unit,
    ): SymbolManager? {
        val current = manager ?: return null
        val recreate = MapLibreStyleLifecycle.mustRecreateAnnotationManager(
            hasManager = true,
            styleContainsManagerLayer = style.getLayer(current.layerId) != null,
        )
        if (!recreate) return current
        runCatching { current.onDestroy() }
        clear()
        return null
    }

    private fun dropLineManagerIfOrphaned(style: Style): LineManager? {
        val current = lineManager ?: return null
        val recreate = MapLibreStyleLifecycle.mustRecreateAnnotationManager(
            hasManager = true,
            styleContainsManagerLayer = style.getLayer(current.layerId) != null,
        )
        if (!recreate) return current
        runCatching { current.onDestroy() }
        lineManager = null
        return null
    }

    fun bindStops(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        stops: List<RouteStop>,
    ) {
        mapRef = map
        MapStopAnnotations.ensureStopMarkerImage(style)
        dropManagerIfOrphaned(symbolManager, style) { symbolManager = null }
        val manager = symbolManager ?: SymbolManager(mapView, map, style).also { created ->
            created.iconAllowOverlap = true
            created.iconIgnorePlacement = true
            created.textAllowOverlap = true
            // OpenFreeMap glyph 404s must not suppress stop icons.
            created.textOptional = MapLibreSymbolText.textOptional
            symbolManager = created
        }
        manager.deleteAll()
        val options = stops.mapIndexedNotNull { index, stop ->
            val lat = stop.latitude ?: return@mapIndexedNotNull null
            val lng = stop.longitude ?: return@mapIndexedNotNull null
            SymbolOptions()
                .withLatLng(LatLng(lat, lng))
                .withIconImage(MapStopAnnotations.ICON_ID)
                .withIconSize(1.1f)
                .withIconAnchor("center")
                .withTextField("${index + 1}")
                // OpenFreeMap hosts Noto Sans only — default Open Sans stack 404s.
                .withTextFont(MapStopAnnotations.symbolTextFont)
                .withTextSize(12f)
                .withTextColor("#FFFFFF")
                .withTextHaloColor("#0B1F33")
                .withTextHaloWidth(1.0f)
                .withTextOffset(arrayOf(0f, 0.05f))
        }
        if (options.isNotEmpty()) {
            manager.create(options)
        }
    }

    fun bindRoutePath(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        points: List<LatLngPoint>,
    ) {
        mapRef = map
        dropLineManagerIfOrphaned(style)
        val manager = lineManager ?: LineManager(mapView, map, style).also { lineManager = it }
        manager.deleteAll()
        if (points.size < 2) return
        val latLngs = points.map { LatLng(it.latitude, it.longitude) }
        manager.create(
            LineOptions()
                .withLatLngs(latLngs)
                .withLineColor(MapRoutePathAnnotations.LINE_COLOR_HEX)
                .withLineWidth(MapRoutePathAnnotations.LINE_WIDTH),
        )
    }

    fun bindBridges(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        bridges: List<BridgePoint>,
    ) {
        mapRef = map
        BridgeMarkersCatalog.ensureBridgeMarkerImage(style, context)
        dropManagerIfOrphaned(bridgeSymbolManager, style) { bridgeSymbolManager = null }
        val manager = bridgeSymbolManager ?: SymbolManager(mapView, map, style).also { created ->
            created.iconAllowOverlap = true
            created.iconIgnorePlacement = true
            created.textAllowOverlap = true
            // Keep icons visible even if the basemap has dense symbol collision.
            created.iconOptional = false
            // Clearance textField + missing Open Sans glyphs used to hide the whole symbol.
            created.textOptional = MapLibreSymbolText.textOptional
            bridgeSymbolManager = created
        }
        manager.deleteAll()
        if (bridges.isEmpty()) return
        val app = context.applicationContext as? TruckieApp
        val flags = app?.featureFlags ?: LocalFeatureFlags()
        // ATNA-73: Compose already ran BridgeVisibilityPipeline — bind must not
        // re-apply W5-8 / passage / vehicle-height (no duplex filter).
        val forBind = BridgeVisibilityPipeline.forSymbolBind(bridges)
        val options = forBind.map { bridge ->
            val label = BridgeMarkersCatalog.clearanceLabel(bridge, flags)
            // W5-8 is a diamond road sign — centre on the bridge point (not a pin tip).
            val base = SymbolOptions()
                .withLatLng(LatLng(bridge.latitude, bridge.longitude))
                .withIconImage(BridgeMarkersCatalog.ICON_ID)
                .withIconSize(BridgeMarkersVisibility.iconSize)
                .withIconAnchor("center")
            if (label == null) {
                base
            } else {
                base
                    .withTextField(label)
                    // OpenFreeMap Liberty glyphs: Noto Sans Regular only.
                    .withTextFont(BridgeMarkersVisibility.symbolTextFont)
                    .withTextSize(12f)
                    .withTextColor("#0B1F33")
                    .withTextHaloColor("#FFFFFF")
                    .withTextHaloWidth(1.4f)
                    .withTextOffset(arrayOf(0f, -1.6f))
                    .withTextAnchor("bottom")
            }
        }
        if (options.isNotEmpty()) {
            manager.create(options)
        }
    }

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
        resetAnnotationManagers()
        styleReady = false
        styleLoading = false
        pendingStyleReady.clear()
        boundStyle = null
        hasAppliedInitialCamera = false
        mapRef = null
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
