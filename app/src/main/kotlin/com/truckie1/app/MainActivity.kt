package com.truckie1.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.ClearanceCalculator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as TruckieApp
        val active = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }

        findViewById<TextView>(R.id.activeVehicle).text =
            active?.let { "Active: ${it.rego} · ${it.heightM} m" } ?: "No active vehicle"

        val effective = active?.let {
            ClearanceCalculator.effectiveHeightM(
                vehicleHeightM = it.heightM,
                cargoHeightsM = emptyList(),
                bufferM = app.clearanceBufferM,
            )
        }
        findViewById<TextView>(R.id.effectiveClearance).text =
            effective?.let { "Eff. clearance (empty + buffer): ${"%.2f".format(it)} m" }
                ?: "Set a vehicle profile to compute clearance"

        findViewById<TextView>(R.id.autoStatus).text =
            if (AndroidAutoGate.isEnabled(app.featureFlags)) {
                "Android Auto: available"
            } else if (!AndroidAutoGate.isSupportedApi()) {
                getString(R.string.auto_unavailable)
            } else {
                "Android Auto: flag ${FlagKeys.ANDROID_AUTO} off"
            }

        findViewById<Button>(R.id.planTrip).setOnClickListener {
            toastIfFlag(app, FlagKeys.MULTI_STOP_CARGO, "Trip planner (flagged)")
        }
        findViewById<Button>(R.id.vehicleProfiles).setOnClickListener {
            toastIfFlag(app, FlagKeys.VEHICLE_PROFILES, "Vehicle profiles (flagged)")
        }
        findViewById<Button>(R.id.settings).setOnClickListener {
            Toast.makeText(
                this,
                "Buffer ${app.clearanceBufferM} m (Settings range 0.15–1.0)",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun toastIfFlag(app: TruckieApp, key: String, message: String) {
        val text = if (app.featureFlags.isEnabled(key)) message else "$key is off"
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
