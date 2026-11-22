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
import com.truckie1.maps.MapCameraDefaults
import com.truckie1.maps.MapCameraTarget
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
 * moves the camera with location updates until the driver pans away; Recenter
 * is owned by the parent overlay.
 *
 * Stop markers use the Annotation plugin [SymbolManager] (not deprecated MarkerOptions).
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
    followState: MapFollowState = MapLocationFollow.initial(),
    onFollowStateChange: (MapFollowState) -> Unit = {},
    recenterToken: Int = 0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provider = locationProvider ?: remember { FusedCurrentLocationProvider(context) }
    val ensurer = settingsEnsurer ?: remember { PlayServicesLocationSettingsEnsurer(context) }
    val hostHolder = remember { mutableStateOf<MapLibreHost?>(null) }
    val followStateRef = remember { mutableStateOf(followState) }
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
                val withFix = MapLocationFollow.onLocationFixChanged(
                    followStateRef.value,
                    hasLocationFix = resolved.puck != null,
                )
                publishFollow(withFix)
                if (withFix.shouldMoveCameraWithLocation && resolved.puck != null) {
                    hostHolder.value?.applyFollowCamera(
                        target = resolved.camera,
                        animate = false,
                    )
                } else if (withFix.isFollowing && resolved.puck == null) {
                    hostHolder.value?.applyCamera(resolved.camera)
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

    // One-shot Recenter animation when the parent increments [recenterToken].
    LaunchedEffect(recenterToken) {
        if (recenterToken <= 0) return@LaunchedEffect
        if (!locationFollowEnabled || !showLocationPuck) return@LaunchedEffect
        val fix = locationFix ?: return@LaunchedEffect
        val target = MapCameraTarget.resolve(fix.latitude, fix.longitude)
        cameraTarget = target
        hostHolder.value?.applyFollowCamera(target = target, animate = true)
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
                map.setStyle(Style.Builder().fromUri(MapCameraDefaults.STYLE_URI)) { style ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(cameraTarget.latitude, cameraTarget.longitude))
                        .zoom(cameraTarget.zoom)
                        .build()
                    mapHost.bindStops(mapView = view, map = map, style = style, stops = stops)
                    if (showLocationPuck) {
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
                            mapHost.setCameraFollowing(followStateRef.value.isFollowing)
                        }
                    } else {
                        mapHost.updatePuck(null, enabled = false)
                    }
                }
            }
        },
    )
}

/**
 * Holds [SymbolManager] and MapLibre [org.maplibre.android.location.LocationComponent]
 * across Compose recompositions.
 */
private class MapLibreHost(
    val mapView: MapView,
    private val context: android.content.Context,
) {
    private var symbolManager: SymbolManager? = null
    private var mapRef: MapLibreMap? = null
    private var locationActivated: Boolean = false
    private var followGesturesBound: Boolean = false
    private var ignoreTrackingDismiss: Boolean = false

    fun bindStops(
        mapView: MapView,
        map: MapLibreMap,
        style: Style,
        stops: List<RouteStop>,
    ) {
        mapRef = map
        MapStopAnnotations.ensureStopMarkerImage(style)
        val manager = symbolManager ?: SymbolManager(mapView, map, style).also { created ->
            created.iconAllowOverlap = true
            created.iconIgnorePlacement = true
            created.textAllowOverlap = true
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
        component.renderMode = RenderMode.NORMAL
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
            component.forceLocationUpdate(fix.toAndroidLocation())
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

    fun applyFollowCamera(target: MapCameraTarget, animate: Boolean) {
        val map = mapRef ?: return
        val latLng = LatLng(target.latitude, target.longitude)
        ignoreTrackingDismiss = true
        runCatching {
            if (animate) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, target.zoom),
                    RECENTER_ANIMATION_MS,
                )
            } else {
                map.cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(target.zoom)
                    .build()
            }
            setCameraFollowing(true)
        }
        mapView.postDelayed({ ignoreTrackingDismiss = false }, RECENTER_ANIMATION_MS.toLong() + 50L)
    }

    fun setCameraFollowing(following: Boolean) {
        if (!locationActivated) return
        val map = mapRef ?: return
        runCatching {
            map.locationComponent.cameraMode = if (following) {
                CameraMode.TRACKING
            } else {
                CameraMode.NONE
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
        runCatching { symbolManager?.onDestroy() }
        symbolManager = null
        if (locationActivated) {
            runCatching {
                mapRef?.locationComponent?.isLocationComponentEnabled = false
                mapRef?.locationComponent?.onDestroy()
            }
            locationActivated = false
        }
        followGesturesBound = false
        mapRef = null
    }

    companion object {
        const val RECENTER_ANIMATION_MS: Int = 750
    }
}

private fun LocationFix.toAndroidLocation(): Location =
    Location("fused").also { loc ->
        loc.latitude = latitude
        loc.longitude = longitude
        accuracyMeters?.let { loc.accuracy = it }
    }
