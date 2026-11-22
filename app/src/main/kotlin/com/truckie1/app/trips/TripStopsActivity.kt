package com.truckie1.app.trips

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.TripStopsScreen
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys
import com.truckie1.routing.OptimizeResult
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop

class TripStopsActivity : ComponentActivity() {
    private var stops by mutableStateOf<List<TripStop>>(emptyList())
    private var clearanceSummary by mutableStateOf("")
    private var showOptimize by mutableStateOf(false)
    private var optimizeMode by mutableStateOf(OptimizeMode.FASTEST)
    private var optimizeStatus by mutableStateOf<String?>(null)
    private var navChromeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.MULTI_STOP)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.MULTI_STOP), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        showOptimize = OptimizeForVehicleGate.isAvailable(app.featureFlags)
        navChromeEnabled = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.trip_stops),
                activitySimpleName = "TripStopsActivity",
                navChromeEnabled = navChromeEnabled,
                onNavigate = { navigateToPrimaryDestination(it) },
            ) {
                TripStopsScreen(
                    stops = stops,
                    clearanceSummary = clearanceSummary,
                    showOptimize = showOptimize,
                    optimizeMode = optimizeMode,
                    optimizeStatus = optimizeStatus,
                    onAddStop = { startActivity(Intent(this@TripStopsActivity, AddStopActivity::class.java)) },
                    onMoveUp = { index ->
                        if (index > 0) {
                            app.tripSession.moveStop(index, index - 1)
                            bindList()
                        }
                    },
                    onMoveDown = { index ->
                        if (index < stops.lastIndex) {
                            app.tripSession.moveStop(index, index + 1)
                            bindList()
                        }
                    },
                    onDelete = { stop ->
                        app.tripSession.removeStop(stop.id)
                        bindList()
                    },
                    onModeChange = { optimizeMode = it },
                    onOptimize = { runOptimize() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!(application as TruckieApp).featureFlags.isEnabled(FlagKeys.MULTI_STOP)) return
        navChromeEnabled = (application as TruckieApp).featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        bindList()
    }

    private fun runOptimize() {
        val app = application as TruckieApp
        if (!OptimizeForVehicleGate.isAvailable(app.featureFlags)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.ROUTE_OPTIMIZE), Toast.LENGTH_SHORT).show()
            return
        }
        val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
        if (vehicle == null) {
            Toast.makeText(this, getString(R.string.optimize_need_vehicle), Toast.LENGTH_SHORT).show()
            return
        }
        val currentStops = app.tripSession.stops()
        if (currentStops.isEmpty()) {
            Toast.makeText(this, getString(R.string.optimize_need_stops), Toast.LENGTH_SHORT).show()
            return
        }

        when (
            val result = app.routeOptimizer.optimize(
                stops = currentStops,
                vehicle = vehicle,
                bufferM = app.clearanceBufferM,
                mode = optimizeMode,
            )
        ) {
            is OptimizeResult.Ok -> {
                app.tripSession.replaceStops(result.orderedStops)
                app.tripSession.lastOptimizeMode = result.mode
                optimizeStatus = getString(
                    R.string.optimize_success,
                    result.mode.name.lowercase().replaceFirstChar { it.titlecase() },
                    result.route.durationSeconds,
                )
                bindList()
            }
            is OptimizeResult.Blocked -> {
                startActivity(
                    Intent(this, BlockedRouteActivity::class.java)
                        .putExtra(BlockedRouteActivity.EXTRA_REASON, result.reason),
                )
            }
        }
    }

    private fun bindList() {
        val app = application as TruckieApp
        val current = app.tripSession.stops()
        stops = current
        clearanceSummary = clearanceText(app, current)
    }

    private fun clearanceText(app: TruckieApp, stops: List<TripStop>): String {
        val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
        if (vehicle == null) return getString(R.string.set_vehicle_for_clearance)
        val effective = if (app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)) {
            ClearanceCalculator.maxEffectiveHeightAlongTripM(
                vehicleHeightM = vehicle.heightM,
                stops = stops,
                bufferM = app.clearanceBufferM,
            )
        } else {
            ClearanceCalculator.effectiveHeightM(
                vehicleHeightM = vehicle.heightM,
                cargoHeightsM = emptyList(),
                bufferM = app.clearanceBufferM,
            )
        }
        return getString(R.string.trip_eff_clearance, "%.2f".format(effective))
    }
}
