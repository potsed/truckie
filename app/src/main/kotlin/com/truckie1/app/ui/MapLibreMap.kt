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
 * When [headingUpEnabled] is true (`ff_map_heading_up`), follow uses
 * [CameraMode.TRACKING_GPS] so the map orients to the direction of travel.
 *
 * Stop markers use the Annotation plugin [SymbolManager] (not deprecated MarkerOptions).
 * When [showBridgeMarkers] is true (`ff_bridge_markers`), a separate SymbolManager
 * draws Australia-first bridge fixtures with optional clearance labels.
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provider = locationProvider ?: remember { FusedCurrentLocationProvider(context) }
    val ensurer = settingsEnsurer ?: remember { PlayServicesLocationSettingsEnsurer(context) }
    val hostHolder = remember { mutableStateOf<MapLibreHost?>(null) }
    val followStateRef = remember { mutableStateOf(followState) }
    SideEffect { followStateRef.value = followState }
    SideEffect {
        val host = hostHolder.value ?: return@SideEffect
        host.headingUpEnabled = headingUpEnabled
        if (recenterToken > 0) {
            host.armRecenterIfNeeded(recenterToken)
        }
        // Only enable when LocationComponent is not already tracking — avoid re-calling
        // setCameraMode every recomposition (was a second camera path).
        if (locationFollowEnabled &&
            MapLocationFollow.shouldEnableFollowMode(
                isFollowing = followState.isFollowing,
                recenterInFlight = host.isRecenterInFlight,
                locationComponentAlreadyTracking = host.isLocationTracking,
            )
        ) {
            host.setCameraFollowing(true)
        }
    }

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

    fun onLocationReady(fix: LocationFix?) {
        val permitted = FusedCurrentLocationProvider.hasLocationPermission(context)
        hasPermission = permitted
        val followActive = locationFollowEnabled && showLocationPuck
        if (showLocationPuck) {
            val resolved = MapLocationPuck.resolve(fix = fix, hasLocationPermission = permitted)
            cameraTarget = resolved.camera
            locationFix = resolved.puck
            hostHolder.value?.updatePuck(resolved.puck, enabled = resolved.puck != null)
            if (followActive) {
                val host = hostHolder.value
                val withFix = MapLocationFollow.onLocationFixChanged(
                    followStateRef.value,
                    hasLocationFix = resolved.puck != null,
                )
                publishFollow(withFix)
                // Steady follow (ATNA-66): LocationComponent TRACKING owns the camera
                // via forceLocationUpdate — no GPS moveCamera (keeps Recenter single-path).
                when (
                    MapLocationFollow.steadyFollowCameraAction(
                        isFollowing = withFix.isFollowing,
                        hasLocationFix = resolved.puck != null,
                        recenterInFlight = host?.isRecenterInFlight == true,
                        locationComponentAlreadyTracking = host?.isLocationTracking == true,
                    )
                ) {
                    SteadyFollowCameraAction.EnableTracking -> host?.setCameraFollowing(true)
                    SteadyFollowCameraAction.RelyOnTracking -> Unit
                    SteadyFollowCameraAction.None ->
                        if (withFix.isFollowing && resolved.puck == null) {
                            host?.applyCamera(resolved.camera)
                        }
                }
            } else {
                hostHolder.value?.applyCamera(cameraTarget)
            }
        } else {
            cameraTarget = MapCameraTarget.resolve(fix?.latitude, fix?.longitude)
            locationFix = null
            hostHolder.value?.updatePuck(null, enabled = false)
            hostHolder.value?.applyCamera(cameraTarget)
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

    // One-shot Recenter: single animateCamera to lat/lng+zoom, then enter follow (ATNA-61).
    LaunchedEffect(recenterToken) {
        if (recenterToken <= 0) return@LaunchedEffect
        if (!locationFollowEnabled || !showLocationPuck) return@LaunchedEffect
        val fix = locationFix ?: return@LaunchedEffect
        val target = MapLocationFollow.cameraTargetForRecenter(fix.latitude, fix.longitude)
        cameraTarget = target
        hostHolder.value?.applyRecenter(target)
    }

    LaunchedEffect(followState.isFollowing, locationFollowEnabled) {
        if (!locationFollowEnabled || !showLocationPuck) return@LaunchedEffect
        if (!followState.isFollowing) {
            hostHolder.value?.setCameraFollowing(false)
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
                        mapHost.updatePuck(locationFix, enabled = locationFix != null)
                        if (locationFollowEnabled) {
                            mapHost.bindFollowGestures(
                                map = map,
                                onUserPanAway = {
                                    publishFollow(
                                        MapLocationFollow.onUserPan(followStateRef.value),
                                    )
                                },
                            )
                            if (MapLocationFollow.shouldEnableFollowMode(
                                    isFollowing = followStateRef.value.isFollowing,
                                    recenterInFlight = mapHost.isRecenterInFlight,
                                    locationComponentAlreadyTracking = mapHost.isLocationTracking,
                                )
                            ) {
                                mapHost.setCameraFollowing(true)
                            } else if (!followStateRef.value.isFollowing &&
                                mapHost.isLocationTracking
                            ) {
                                mapHost.setCameraFollowing(false)
                            }
                        }
                    } else {
                        mapHost.updatePuck(null, enabled = false)
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
    private var lastArmedRecenterToken: Int = 0
    private var styleReady: Boolean = false
    private var styleLoading: Boolean = false
    private val pendingStyleReady = mutableListOf<(Style) -> Unit>()
    /** Style instance managers were last created against (identity check). */
    private var boundStyle: Style? = null
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
                    styleReady = true
                    styleLoading = false
                    val callbacks = pendingStyleReady.toList()
                    pendingStyleReady.clear()
                    callbacks.forEach { callback -> callback(style) }
                }
            }
        }
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
        // Callers should already filter via LowBridgeW58; re-apply so W5-8 is
        // never drawn on unknown / high / not_applicable spans (ATNA-45).
        val forW58 = LowBridgeW58.bridgesForW58(bridges, flags)
        // Under-restricting structures only when passage flag on (ATNA-48).
        val forUnder = BridgePassageFilter.bridgesRestrictingUnder(forW58, flags)
        // Conflicts only for active vehicle height + system buffer (ATNA-47).
        val vehicleHeightM = app?.vehicleProfiles?.activeRego
            ?.let { rego -> app.vehicleProfiles.findByRego(rego)?.heightM }
        val bufferM = app?.clearanceBufferM
            ?: com.truckie1.trips.ClearanceBuffer.DEFAULT_M
        val forVehicle = VehicleHeightBridgeFilter.bridgesAffectingVehicle(
            bridges = forUnder,
            flags = flags,
            vehicleHeightM = vehicleHeightM,
            bufferM = bufferM,
        )
        val options = forVehicle.map { bridge ->
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

    fun updatePuck(fix: LocationFix?, enabled: Boolean) {
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

    fun applyCamera(target: MapCameraTarget) {
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

    fun armRecenterIfNeeded(recenterToken: Int) {
        if (recenterToken <= lastArmedRecenterToken) return
        lastArmedRecenterToken = recenterToken
        ignoreTrackingDismiss = true
    }

    /**
     * Single Recenter path (ATNA-61 / ATNA-66): exit TRACKING → one
     * animateCamera(latLng+zoom) → sync last location → bare cameraMode follow enter
     * (no second zoom, no 0ms timed overload stick risk).
     */
    fun applyRecenter(target: MapCameraTarget) {
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
     * Uses a **bare** `cameraMode = TRACKING|TRACKING_GPS` set (ATNA-66). After Recenter
     * animate + lastLocation sync, MapLibre takes the immediateAnimation path — no second
     * zoom. Avoids `setCameraMode(mode, 0, null…)` which can leave isTransitioning stuck
     * so later forceLocationUpdate moves the puck but not the camera.
     */
    fun setCameraFollowing(
        following: Boolean,
        fromRecenterFinish: Boolean = false,
    ) {
        if (!locationActivated) return
        // SideEffect / AndroidView must not steal the camera mid-Recenter animate.
        if (following && ignoreTrackingDismiss && !fromRecenterFinish) return
        val map = mapRef ?: return
        runCatching {
            val component = map.locationComponent
            if (following) {
                component.renderMode =
                    MapLocationFollow.followingRenderModeInt(headingUpEnabled)
                val enter = MapLocationFollow.followEnterAfterFramedRecenter()
                if (enter.usesBareCameraModeSetter) {
                    // Bare setter — no timed overload, no zoom arg (animateCamera owns zoom).
                    component.cameraMode =
                        MapLocationFollow.followingCameraModeInt(headingUpEnabled)
                } else {
                    // Defensive: never use 0ms timed overload (stick risk).
                    val ms = enter.timedTransitionMs?.takeIf { it > 0L } ?: return@runCatching
                    component.setCameraMode(
                        MapLocationFollow.followingCameraModeInt(headingUpEnabled),
                        ms,
                        /* zoom */ null,
                        /* bearing */ null,
                        /* tilt */ null,
                        /* listener */ null,
                    )
                }
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
