package com.truckie1.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.profiles.HomeVehicleSummary
import com.truckie1.app.trips.HomeTripSummary
import com.truckie1.app.ui.HomeScreen
import com.truckie1.app.ui.HomeUiState
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.TruckiePrimaryDestination
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys

class MainActivity : ComponentActivity() {
    private var uiState by mutableStateOf(
        HomeUiState(
            activeVehicle = null,
            clearanceLine = null,
            tripSummary = null,
            autoStatus = "",
            showPlanTrip = false,
            showVehicleProfiles = false,
        ),
    )
    private var navChromeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navChromeEnabled = (application as TruckieApp).featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.home_title),
                activitySimpleName = "MainActivity",
                navChromeEnabled = navChromeEnabled,
                onNavigate = { navigateToPrimaryDestination(it) },
            ) {
                HomeScreen(
                    state = uiState,
                    onPlanTrip = {
                        navigateToPrimaryDestination(TruckiePrimaryDestination.PLAN_TRIP)
                    },
                    onVehicleProfiles = {
                        navigateToPrimaryDestination(TruckiePrimaryDestination.VEHICLES)
                    },
                    onSettings = {
                        if (navChromeEnabled) {
                            navigateToPrimaryDestination(TruckiePrimaryDestination.SETTINGS)
                        } else {
                            val app = application as TruckieApp
                            Toast.makeText(
                                this@MainActivity,
                                "Buffer ${app.clearanceBufferM} m (Settings range 0.15–1.0)",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bindHome()
    }

    private fun bindHome() {
        val app = application as TruckieApp
        navChromeEnabled = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        val activeSummary = HomeVehicleSummary.text(app.featureFlags, app.vehicleProfiles)
        val active = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
        val clearanceLine = if (activeSummary == null) {
            null
        } else {
            HomeTripSummary.clearanceLine(
                flags = app.featureFlags,
                session = app.tripSession,
                vehicleHeightM = active?.heightM,
                bufferM = app.clearanceBufferM,
            ) ?: getString(R.string.set_vehicle_for_clearance)
        }
        val autoStatus = when {
            AndroidAutoGate.isEnabled(app.featureFlags) -> "Android Auto: available"
            !AndroidAutoGate.isSupportedApi() -> getString(R.string.auto_unavailable)
            else -> "Android Auto: flag ${FlagKeys.ANDROID_AUTO} off"
        }
        uiState = HomeUiState(
            activeVehicle = activeSummary,
            clearanceLine = clearanceLine,
            tripSummary = HomeTripSummary.text(app.featureFlags, app.tripSession),
            autoStatus = autoStatus,
            showPlanTrip = app.featureFlags.isEnabled(FlagKeys.MULTI_STOP),
            showVehicleProfiles = app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES),
        )
    }
}
