package com.truckie1.app

import android.content.Intent
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
import com.truckie1.app.ui.TruckieRootApp
import com.truckie1.app.ui.TruckieRoutes
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys

/**
 * Launcher Activity. With [FlagKeys.NAV_CHROME] on, this is the **single** host for
 * global fixed TopAppBar + bottom NavigationBar around a Compose NavHost.
 */
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
    private var pendingRoute by mutableStateOf(TruckieRoutes.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        // Map-first home requires the single-activity NavHost; treat as chrome-on.
        navChromeEnabled = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME) ||
            app.featureFlags.isEnabled(FlagKeys.MAP_HOME)
        pendingRoute = resolveStartRoute(intent)
        setTruckieContent {
            if (navChromeEnabled) {
                TruckieRootApp(
                    app = app,
                    activity = this@MainActivity,
                    startRoute = pendingRoute,
                )
            } else {
                TruckieHostedScreen(
                    title = getString(R.string.home_title),
                    activitySimpleName = "MainActivity",
                    navChromeEnabled = false,
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
                            Toast.makeText(
                                this@MainActivity,
                                "Buffer ${app.clearanceBufferM} m (Settings range 0.15–1.0)",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = resolveStartRoute(intent)
    }

    override fun onResume() {
        super.onResume()
        val app = application as TruckieApp
        navChromeEnabled = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        if (!navChromeEnabled) {
            bindHomeLegacy()
        }
    }

    private fun resolveStartRoute(intent: Intent?): String {
        val extra = intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() }
        return extra ?: TruckieRoutes.HOME
    }

    private fun bindHomeLegacy() {
        val app = application as TruckieApp
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

    companion object {
        const val EXTRA_ROUTE = "truckie_route"
    }
}
