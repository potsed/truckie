package com.truckie1.app.trips

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.CargoAction

class AddStopActivity : AppCompatActivity() {
    private val pendingCargo = mutableListOf<StopFormParser.CargoInput>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.MULTI_STOP)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.MULTI_STOP), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContentView(R.layout.activity_add_stop)

        val cargoEnabled = app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)
        findViewById<LinearLayout>(R.id.cargoSection).visibility =
            if (cargoEnabled) View.VISIBLE else View.GONE

        findViewById<Button>(R.id.addAnotherCargo).setOnClickListener {
            if (!cargoEnabled) return@setOnClickListener
            stageCurrentCargoRow()?.let { pendingCargo += it }
            clearCargoFields()
            bindPendingCargo()
        }

        findViewById<Button>(R.id.saveStop).setOnClickListener { save(cargoEnabled) }
    }

    private fun save(cargoEnabled: Boolean) {
        val cargoItems = pendingCargo.toMutableList()
        if (cargoEnabled) {
            stageCurrentCargoRow()?.let { cargoItems += it }
        }
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = text(R.id.fieldAddress),
                cargoEnabled = cargoEnabled,
                cargoItems = cargoItems,
            ),
        )
        when (result) {
            is StopFormParser.Result.Err ->
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            is StopFormParser.Result.Ok -> {
                val app = application as TruckieApp
                app.tripSession.addStop(result.stop)
                Toast.makeText(this, getString(R.string.saved_stop, result.stop.addressLabel), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun stageCurrentCargoRow(): StopFormParser.CargoInput? {
        val description = text(R.id.fieldCargoDescription)
        val height = text(R.id.fieldCargoHeight)
        val width = text(R.id.fieldCargoWidth)
        val length = text(R.id.fieldCargoLength)
        val mass = text(R.id.fieldCargoMass)
        if (description.isBlank() && height.isBlank() && width.isBlank() && length.isBlank() && mass.isBlank()) {
            return null
        }
        val action = if (findViewById<RadioButton>(R.id.actionDropOff).isChecked) {
            CargoAction.DROP_OFF.name
        } else {
            CargoAction.PICK_UP.name
        }
        return StopFormParser.CargoInput(
            action = action,
            description = description,
            heightM = height,
            widthM = width,
            lengthM = length,
            massT = mass,
        )
    }

    private fun clearCargoFields() {
        findViewById<EditText>(R.id.fieldCargoDescription).setText("")
        findViewById<EditText>(R.id.fieldCargoHeight).setText("")
        findViewById<EditText>(R.id.fieldCargoWidth).setText("")
        findViewById<EditText>(R.id.fieldCargoLength).setText("")
        findViewById<EditText>(R.id.fieldCargoMass).setText("")
        findViewById<RadioButton>(R.id.actionPickUp).isChecked = true
    }

    private fun bindPendingCargo() {
        val lines = pendingCargo.mapIndexed { index, cargo ->
            val action = if (cargo.action == CargoAction.DROP_OFF.name) "drop" else "pick"
            "${index + 1}. $action · ${cargo.description} · ${cargo.heightM}m / ${cargo.massT}t"
        }
        findViewById<TextView>(R.id.pendingCargoList).text = lines.joinToString("\n")
    }

    private fun text(id: Int): String = findViewById<EditText>(id).text?.toString().orEmpty()
}
