package com.truckie1.app.trips

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.ui.AddStopFormState
import com.truckie1.app.ui.AddStopScreen
import com.truckie1.app.ui.TruckieHostedScreen
import com.truckie1.app.ui.TruckieRoutes
import com.truckie1.app.ui.navigateToPrimaryDestination
import com.truckie1.app.ui.redirectToChromeHostIfEnabled
import com.truckie1.app.ui.setTruckieContent
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.CargoAction

/** Legacy host when `ff_nav_chrome` is off; redirects into root NavHost when on. */
class AddStopActivity : ComponentActivity() {
    private val pendingCargo = mutableListOf<StopFormParser.CargoInput>()
    private var form by mutableStateOf(AddStopFormState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TruckieApp
        if (!app.featureFlags.isEnabled(FlagKeys.MULTI_STOP)) {
            Toast.makeText(this, getString(R.string.flag_off, FlagKeys.MULTI_STOP), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (redirectToChromeHostIfEnabled(TruckieRoutes.ADD_STOP)) {
            finish()
            return
        }
        val cargoEnabled = app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)
        form = form.copy(cargoEnabled = cargoEnabled)

        setTruckieContent {
            TruckieHostedScreen(
                title = getString(R.string.add_stop_title),
                activitySimpleName = "AddStopActivity",
                navChromeEnabled = false,
                onNavigate = { navigateToPrimaryDestination(it) },
                scrollable = true,
            ) {
                AddStopScreen(
                    state = form,
                    onChange = { form = it },
                    onAddAnotherCargo = {
                        if (!cargoEnabled) return@AddStopScreen
                        stageCurrentCargoRow()?.let { pendingCargo += it }
                        form = form.copy(
                            cargoDescription = "",
                            cargoHeightM = "",
                            cargoWidthM = "",
                            cargoLengthM = "",
                            cargoMassT = "",
                            cargoAction = CargoAction.PICK_UP,
                            pendingCargoLines = pendingCargoLines(),
                        )
                    },
                    onSave = { save(cargoEnabled) },
                )
            }
        }
    }

    private fun save(cargoEnabled: Boolean) {
        val cargoItems = pendingCargo.toMutableList()
        if (cargoEnabled) {
            stageCurrentCargoRow()?.let { cargoItems += it }
        }
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = form.address,
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
        val description = form.cargoDescription
        val height = form.cargoHeightM
        val width = form.cargoWidthM
        val length = form.cargoLengthM
        val mass = form.cargoMassT
        if (description.isBlank() && height.isBlank() && width.isBlank() && length.isBlank() && mass.isBlank()) {
            return null
        }
        return StopFormParser.CargoInput(
            action = form.cargoAction.name,
            description = description,
            heightM = height,
            widthM = width,
            lengthM = length,
            massT = mass,
        )
    }

    private fun pendingCargoLines(): List<String> = pendingCargo.mapIndexed { index, cargo ->
        val action = if (cargo.action == CargoAction.DROP_OFF.name) "drop" else "pick"
        "${index + 1}. $action · ${cargo.description} · ${cargo.heightM}m / ${cargo.massT}t"
    }
}
