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
 * Stop markers (`ff_stop_markers`) and bridge markers (`ff_bridge_markers`) both
 * use the Annotation plugin [SymbolManager] via the shared [MapSymbolBind] /
 * [MapLibreStyleLifecycle] path (not deprecated MarkerOptions, not a parallel
 * marker stack). Pickup vs drop-off use distinct icons; bridges use W5-8.
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
    /**
     * Fused fix for route-path approach (ATNA-99). Hosts may feed
     * [ActiveTourPath][com.truckie1.app.routing.ActiveTourPath] without a
     * second location stack.
     */
    onLocationFixChange: (LocationFix?) -> Unit = {},
    recenterToken: Int = 0,
    /** GraphHopper path points drawn on this same map (Navigate). */
    routePathPoints: List<LatLngPoint> = emptyList(),
    bridges: List<BridgePoint> = emptyList(),
    showBridgeMarkers: Boolean = false,
    /** Route stop markers (`ff_stop_markers`) — pickup vs drop-off icons. */
    showStopMarkers: Boolean = false,
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
        onLocationFixChange(if (permitted) fix else null)
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
                    if (!mapHost.hasAppliedInitialCamera) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(cameraTarget.latitude, cameraTarget.longitude))
                            .zoom(cameraTarget.zoom)
                            .build()
                        mapHost.hasAppliedInitialCamera = true
                    }
                    // Compose builds the snapshot (incl. pre-labelled bridges) —
                    // host only executes (ATNA-90).
                    val appFlags = (context.applicationContext as? TruckieApp)
                        ?.featureFlags
                        ?: LocalFeatureFlags()
                    val snapshot = MapLibreBindSnapshot(
                        stops = if (showStopMarkers) stops else emptyList(),
                        routePathPoints = routePathPoints,
                        bridges = if (showBridgeMarkers) {
                            MapLibreBindSnapshot.bridgeSpecs(bridges, appFlags)
                        } else {
                            emptyList()
                        },
                        prioritiseStreetLabels = prioritiseStreetLabels,
                        showLocationPuck = showLocationPuck,
                        locationFollowEnabled = locationFollowEnabled,
                        headingUpEnabled = headingUpEnabled,
                        locationFix = locationFix,
                        cameraTarget = cameraTarget,
                        isFollowing = followStateRef.value.isFollowing,
                    )
                    // Annotations first (stops + path); bridges rebound after puck so
                    // W5-8 sits above LocationComponent layers.
                    mapHost.applyStreetLabelPriority(
                        style = style,
                        enabled = snapshot.prioritiseStreetLabels,
                    )
                    mapHost.bindStops(
                        mapView = view,
                        map = map,
                        style = style,
                        stops = snapshot.stops,
                    )
                    mapHost.bindRoutePath(
                        mapView = view,
                        map = map,
                        style = style,
                        points = snapshot.routePathPoints,
                    )
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
                        bridges = snapshot.bridges,
                    )
                }
            }
        },
    )
}

