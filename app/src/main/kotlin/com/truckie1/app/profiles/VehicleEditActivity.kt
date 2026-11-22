package com.truckie1.app.profiles

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.VehicleEditFormState
import com.truckie1.app.ui.VehicleEditScreen
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle

class VehicleEditActivity : ComponentActivity() {
    private var form by mutableStateOf(VehicleEditFormState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val existingRego = intent.getStringExtra(EXTRA_REGO)
        val existing = existingRego?.let { app.vehicleProfiles.findByRego(it) }
        if (existing != null) {
            form = existing.toFormState(regoEnabled = false)
        }

        setTruckieContent {
            VehicleEditScreen(
                state = form,
                onChange = { form = it },
                onSave = { save() },
            )
        }
    }

    private fun save() {
        val state = form
        val result = VehicleFormParser.parse(
            VehicleFormParser.Input(
                rego = state.rego,
                nickname = state.nickname,
                vehicleType = state.vehicleType,
                make = state.make,
                model = state.model,
                year = state.year,
                heightM = state.heightM,
                widthM = state.widthM,
                lengthM = state.lengthM,
                massT = state.massT,
                tareT = state.tareT,
                gvmT = state.gvmT,
                gcmT = state.gcmT,
                axles = state.axles,
                networkClass = state.networkClass,
            ),
        )
        when (result) {
            is VehicleFormParser.Result.Err ->
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            is VehicleFormParser.Result.Ok -> {
                val app = application as TruckieApp
                app.vehicleProfiles.save(result.vehicle)
                Toast.makeText(this, getString(R.string.saved_vehicle, result.vehicle.rego), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_REGO = "rego"
    }
}

private fun Vehicle.toFormState(regoEnabled: Boolean): VehicleEditFormState = VehicleEditFormState(
    rego = rego,
    nickname = nickname,
    vehicleType = vehicleType,
    make = make,
    model = model,
    year = year.toString(),
    heightM = heightM.toString(),
    widthM = widthM.toString(),
    lengthM = lengthM.toString(),
    massT = massT.toString(),
    tareT = tareT.toString(),
    gvmT = gvmT.toString(),
    gcmT = gcmT.toString(),
    axles = axleCount.toString(),
    networkClass = networkClass.orEmpty(),
    regoEnabled = regoEnabled,
)
