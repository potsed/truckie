package com.truckie1.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
)

/**
 * Official Play Services / Fused Location Provider stack for the map-home camera
 * and my-location puck. Callers must hold fine or coarse location permission
 * before [fetchCurrent] / [startUpdates].
 */
interface CurrentLocationProvider {
    /**
     * Fetches a single current fix via [com.google.android.gms.location.FusedLocationProviderClient].
     * Invokes [onResult] on the main thread with a fix, or null on failure / timeout / unavailable.
     */
    fun fetchCurrent(onResult: (LocationFix?) -> Unit)

    /**
     * Starts continuous fused updates. No-op when permission is missing.
     * Invokes [onUpdate] on the main thread for each fix.
     */
    fun startUpdates(onUpdate: (LocationFix) -> Unit) {}

    /** Stops continuous fused updates previously started by [startUpdates]. */
    fun stopUpdates() {}
}

/**
 * Ensures device location settings can satisfy a fused request; may surface the
 * standard Google Play services resolution dialog via [onResolutionRequired].
 */
fun interface LocationSettingsEnsurer {
    fun ensureSettingsSatisfied(
        onReady: () -> Unit,
        onUnavailable: () -> Unit,
        onResolutionRequired: (IntentSenderRequest) -> Unit,
    )
}

class FusedCurrentLocationProvider(
    private val context: Context,
    private val executor: Executor = ContextCompat.getMainExecutor(context),
) : CurrentLocationProvider {

    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val activeCallback = AtomicReference<LocationCallback?>(null)

    @SuppressLint("MissingPermission")
    override fun fetchCurrent(onResult: (LocationFix?) -> Unit) {
        if (!hasLocationPermission(context)) {
            onResult(null)
            return
        }
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(60_000L)
            .build()
        val token = CancellationTokenSource()
        fused.getCurrentLocation(request, token.token)
            .addOnSuccessListener(executor) { location ->
                onResult(location?.toLocationFix())
            }
            .addOnFailureListener(executor) {
                fused.lastLocation
                    .addOnSuccessListener(executor) { last ->
                        onResult(last?.toLocationFix())
                    }
                    .addOnFailureListener(executor) { onResult(null) }
            }
    }

    @SuppressLint("MissingPermission")
    override fun startUpdates(onUpdate: (LocationFix) -> Unit) {
        if (!hasLocationPermission(context)) return
        stopUpdates()
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_M)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onUpdate(location.toLocationFix())
            }
        }
        activeCallback.set(callback)
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    override fun stopUpdates() {
        activeCallback.getAndSet(null)?.let { callback ->
            runCatching { fused.removeLocationUpdates(callback) }
        }
    }

    companion object {
        const val UPDATE_INTERVAL_MS: Long = 5_000L
        const val MIN_UPDATE_INTERVAL_MS: Long = 2_000L
        const val MIN_UPDATE_DISTANCE_M: Float = 5f

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }
    }
}

private fun android.location.Location.toLocationFix(): LocationFix =
    LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    )

class PlayServicesLocationSettingsEnsurer(
    private val context: Context,
    private val executor: Executor = ContextCompat.getMainExecutor(context),
) : LocationSettingsEnsurer {

    private val settingsClient = LocationServices.getSettingsClient(context)

    override fun ensureSettingsSatisfied(
        onReady: () -> Unit,
        onUnavailable: () -> Unit,
        onResolutionRequired: (IntentSenderRequest) -> Unit,
    ) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L,
        ).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
            .build()
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener(executor) { onReady() }
            .addOnFailureListener(executor) { error ->
                if (error is ResolvableApiException) {
                    try {
                        onResolutionRequired(
                            IntentSenderRequest.Builder(error.resolution).build(),
                        )
                    } catch (_: IntentSender.SendIntentException) {
                        onUnavailable()
                    }
                } else {
                    onUnavailable()
                }
            }
    }
}

/** True when [Activity.RESULT_OK] means the user enabled location in the resolution UI. */
fun locationSettingsResolutionSucceeded(resultCode: Int): Boolean =
    resultCode == Activity.RESULT_OK
