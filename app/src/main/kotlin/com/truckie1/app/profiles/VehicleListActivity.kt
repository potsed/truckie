package com.truckie1.app.profiles

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.VehicleListScreen
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle

class VehicleListActivity : ComponentActivity() {
    private var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
    private var activeRego by mutableStateOf<String?>(null)
    private var navChromeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        navChromeEnabled = app.featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.vehicle_profiles),
                activitySimpleName = "VehicleListActivity",
                navChromeEnabled = navChromeEnabled,
                onNavigate = { navigateToPrimaryDestination(it) },
            ) {
                VehicleListScreen(
                    vehicles = vehicles,
                    activeRego = activeRego,
                    onAdd = { startActivity(Intent(this@VehicleListActivity, VehicleEditActivity::class.java)) },
                    onSelect = { vehicle ->
                        app.vehicleProfiles.setActive(vehicle.rego)
                        bindList()
                        Toast.makeText(this@VehicleListActivity, getString(R.string.active_set, vehicle.rego), Toast.LENGTH_SHORT).show()
                    },
                    onEdit = { vehicle ->
                        startActivity(
                            Intent(this@VehicleListActivity, VehicleEditActivity::class.java).putExtra(
                                VehicleEditActivity.EXTRA_REGO,
                                vehicle.rego,
                            ),
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!(application as TruckieApp).featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) return
        navChromeEnabled = (application as TruckieApp).featureFlags.isEnabled(FlagKeys.NAV_CHROME)
        bindList()
    }

    private fun bindList() {
        val app = application as TruckieApp
        vehicles = app.vehicleProfiles.all()
        activeRego = app.vehicleProfiles.activeRego
    }
}
