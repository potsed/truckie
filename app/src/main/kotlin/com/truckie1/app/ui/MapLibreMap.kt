package com.truckie1.app.ui

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
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Full-bleed MapLibre map. Camera prefers the driver's **current location** via
 * Play Services Fused Location Provider (runtime permission + Settings resolution);
 * falls back to Brisbane when unavailable.
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provider = locationProvider ?: remember { FusedCurrentLocationProvider(context) }
    val ensurer = settingsEnsurer ?: remember { PlayServicesLocationSettingsEnsurer(context) }

    var cameraTarget by remember { mutableStateOf(MapCameraTarget.fallback()) }

    fun applyFix(fix: LocationFix?) {
        cameraTarget = MapCameraTarget.resolve(
            latitude = fix?.latitude,
            longitude = fix?.longitude,
        )
    }

    fun fetchAfterSettingsReady() {
        provider.fetchCurrent { fix -> applyFix(fix) }
    }

    val settingsResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (locationSettingsResolutionSucceeded(result.resultCode)) {
            fetchAfterSettingsReady()
        } else {
            applyFix(null)
        }
    }

    fun ensureSettingsThenFetch() {
        ensurer.ensureSettingsSatisfied(
            onReady = { fetchAfterSettingsReady() },
            onUnavailable = { applyFix(null) },
            onResolutionRequired = { request -> settingsResolutionLauncher.launch(request) },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            ensureSettingsThenFetch()
        } else {
            applyFix(null)
        }
    }

    LaunchedEffect(provider, ensurer) {
        if (FusedCurrentLocationProvider.hasLocationPermission(context)) {
            ensureSettingsThenFetch()
        } else {
            permissionLauncher.launch(MapHomeCatalog.locationPermissions.toTypedArray())
        }
    }

    val mapView = remember {
        runCatching {
            MapLibre.getInstance(context.applicationContext)
            MapView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                onCreate(null)
            }
        }.getOrNull()
    }

    if (mapView == null) {
        val label = if (cameraTarget.usedFallback) {
            "MapLibre · Brisbane fallback"
        } else {
            "MapLibre · current location"
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

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onDestroy()
            } catch (_: Throwable) {
                // Already destroyed or unavailable in test hosts.
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
                map.setStyle(Style.Builder().fromUri(MapCameraDefaults.STYLE_URI)) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(cameraTarget.latitude, cameraTarget.longitude))
                        .zoom(cameraTarget.zoom)
                        .build()
                    map.clear()
                    stops.forEachIndexed { index, stop ->
                        val lat = stop.latitude ?: return@forEachIndexed
                        val lng = stop.longitude ?: return@forEachIndexed
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title("${index + 1}. ${stop.addressLabel}")
                                .snippet(stop.kind.name.lowercase().replace('_', '/')),
                        )
                    }
                }
            }
        },
    )
}
