package com.truckie1.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.profiles.HomeVehicleSummary
import com.truckie1.app.profiles.VehicleListActivity
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.ClearanceCalculator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.planTrip).setOnClickListener {
            val app = application as TruckieApp
            toastIfFlag(app, FlagKeys.MULTI_STOP_CARGO, "Trip planner (flagged)")
        }
        findViewById<Button>(R.id.vehicleProfiles).setOnClickListener {
            val app = application as TruckieApp
            if (app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
                startActivity(Intent(this, VehicleListActivity::class.java))
            } else {
                Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.settings).setOnClickListener {
            val app = application as TruckieApp
            Toast.makeText(
                this,
                "Buffer ${app.clearanceBufferM} m (Settings range 0.15–1.0)",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        bindHome()
    }

    private fun bindHome() {
        val app = application as TruckieApp
        val activeSummary = HomeVehicleSummary.text(app.featureFlags, app.vehicleProfiles)
        val activeView = findViewById<TextView>(R.id.activeVehicle)
        val clearanceView = findViewById<TextView>(R.id.effectiveClearance)

        if (activeSummary == null) {
            activeView.visibility = View.GONE
            clearanceView.visibility = View.GONE
        } else {
            activeView.visibility = View.VISIBLE
            clearanceView.visibility = View.VISIBLE
            activeView.text = activeSummary

            val active = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
            val effective = active?.let {
                ClearanceCalculator.effectiveHeightM(
                    vehicleHeightM = it.heightM,
                    cargoHeightsM = emptyList(),
                    bufferM = app.clearanceBufferM,
                )
            }
            clearanceView.text =
                effective?.let { "Eff. clearance (empty + buffer): ${"%.2f".format(it)} m" }
                    ?: "Set a vehicle profile to compute clearance"
        }

        findViewById<TextView>(R.id.autoStatus).text =
            if (AndroidAutoGate.isEnabled(app.featureFlags)) {
                "Android Auto: available"
            } else if (!AndroidAutoGate.isSupportedApi()) {
                getString(R.string.auto_unavailable)
            } else {
                "Android Auto: flag ${FlagKeys.ANDROID_AUTO} off"
            }

        findViewById<Button>(R.id.vehicleProfiles).visibility =
            if (app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) View.VISIBLE else View.GONE
    }

    private fun toastIfFlag(app: TruckieApp, key: String, message: String) {
        val text = if (app.featureFlags.isEnabled(key)) message else "$key is off"
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
