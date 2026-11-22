package com.truckie1.app.profiles

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle

class VehicleEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.VEHICLE_PROFILES), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContentView(R.layout.activity_vehicle_edit)

        val existingRego = intent.getStringExtra(EXTRA_REGO)
        val existing = existingRego?.let { app.vehicleProfiles.findByRego(it) }
        if (existing != null) {
            bind(existing)
            findViewById<EditText>(R.id.fieldRego).isEnabled = false
        }

        findViewById<Button>(R.id.saveVehicle).setOnClickListener { save() }
    }

    private fun bind(vehicle: Vehicle) {
        findViewById<EditText>(R.id.fieldRego).setText(vehicle.rego)
        findViewById<EditText>(R.id.fieldNickname).setText(vehicle.nickname)
        findViewById<EditText>(R.id.fieldVehicleType).setText(vehicle.vehicleType)
        findViewById<EditText>(R.id.fieldMake).setText(vehicle.make)
        findViewById<EditText>(R.id.fieldModel).setText(vehicle.model)
        findViewById<EditText>(R.id.fieldYear).setText(vehicle.year.toString())
        findViewById<EditText>(R.id.fieldHeight).setText(vehicle.heightM.toString())
        findViewById<EditText>(R.id.fieldWidth).setText(vehicle.widthM.toString())
        findViewById<EditText>(R.id.fieldLength).setText(vehicle.lengthM.toString())
        findViewById<EditText>(R.id.fieldMass).setText(vehicle.massT.toString())
        findViewById<EditText>(R.id.fieldTare).setText(vehicle.tareT.toString())
        findViewById<EditText>(R.id.fieldGvm).setText(vehicle.gvmT.toString())
        findViewById<EditText>(R.id.fieldGcm).setText(vehicle.gcmT.toString())
        findViewById<EditText>(R.id.fieldAxles).setText(vehicle.axleCount.toString())
        findViewById<EditText>(R.id.fieldNetwork).setText(vehicle.networkClass.orEmpty())
    }

    private fun save() {
        val result = VehicleFormParser.parse(
            VehicleFormParser.Input(
                rego = text(R.id.fieldRego),
                nickname = text(R.id.fieldNickname),
                vehicleType = text(R.id.fieldVehicleType),
                make = text(R.id.fieldMake),
                model = text(R.id.fieldModel),
                year = text(R.id.fieldYear),
                heightM = text(R.id.fieldHeight),
                widthM = text(R.id.fieldWidth),
                lengthM = text(R.id.fieldLength),
                massT = text(R.id.fieldMass),
                tareT = text(R.id.fieldTare),
                gvmT = text(R.id.fieldGvm),
                gcmT = text(R.id.fieldGcm),
                axles = text(R.id.fieldAxles),
                networkClass = text(R.id.fieldNetwork),
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

    private fun text(id: Int): String = findViewById<EditText>(id).text?.toString().orEmpty()

    companion object {
        const val EXTRA_REGO = "rego"
    }
}
