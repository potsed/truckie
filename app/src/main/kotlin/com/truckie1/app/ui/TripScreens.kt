package com.truckie1.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckie1.app.R
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import com.truckie1.trips.TripStopSummary

@Composable
fun ColumnScope.TripStopsScreen(
    stops: List<TripStop>,
    clearanceSummary: String,
    showOptimize: Boolean,
    optimizeMode: OptimizeMode,
    optimizeStatus: String?,
    onAddStop: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDelete: (TripStop) -> Unit,
    onModeChange: (OptimizeMode) -> Unit,
    onOptimize: () -> Unit,
    /** Optional GraphHopper-optimised order / source line (ATNA-68). Default null keeps RootNav call sites unchanged. */
    optimizeOrderSummary: String? = null,
) {
    TruckieBody(text = clearanceSummary)
    Spacer(modifier = Modifier.height(12.dp))
    if (stops.isEmpty()) {
        TruckieBody(text = stringResource(R.string.no_stops_yet))
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                StopRow(
                    title = TripStopSummary.title(index, stop),
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onDelete = { onDelete(stop) },
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.add_stop),
        onClick = onAddStop,
        modifier = Modifier.fillMaxWidth(),
    )
    if (showOptimize) {
        Spacer(modifier = Modifier.height(12.dp))
        TruckieBody(text = stringResource(R.string.optimize_for_vehicle))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OptimizeModeRadio(
                label = stringResource(R.string.optimize_fastest),
                selected = optimizeMode == OptimizeMode.FASTEST,
                onClick = { onModeChange(OptimizeMode.FASTEST) },
            )
            OptimizeModeRadio(
                label = stringResource(R.string.optimize_cheapest),
                selected = optimizeMode == OptimizeMode.CHEAPEST,
                onClick = { onModeChange(OptimizeMode.CHEAPEST) },
            )
        }
        TruckiePrimaryButton(
            text = stringResource(R.string.run_optimize),
            onClick = onOptimize,
            modifier = Modifier.fillMaxWidth(),
        )
        if (optimizeStatus != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TruckieBody(text = optimizeStatus)
        }
        if (optimizeOrderSummary != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TruckieBody(text = optimizeOrderSummary)
        }
    }
}

@Composable
private fun OptimizeModeRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = TruckiePalette.Navy,
                unselectedColor = TruckiePalette.Navy,
            ),
        )
        Text(text = label, color = TruckiePalette.Navy)
    }
}

@Composable
private fun StopRow(
    title: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, modifier = Modifier.weight(1f), color = TruckiePalette.Navy)
        TruckieTextButton(text = stringResource(R.string.move_up), onClick = onMoveUp)
        TruckieTextButton(text = stringResource(R.string.move_down), onClick = onMoveDown)
        TruckieTextButton(text = stringResource(R.string.delete), onClick = onDelete)
    }
}

data class AddStopFormState(
    val address: String = "",
    val cargoEnabled: Boolean = false,
    val cargoAction: CargoAction = CargoAction.PICK_UP,
    val cargoDescription: String = "",
    val cargoHeightM: String = "",
    val cargoWidthM: String = "",
    val cargoLengthM: String = "",
    val cargoMassT: String = "",
    val pendingCargoLines: List<String> = emptyList(),
)

/**
 * Add-stop form. Address field is search-shaped for a future **verified autocomplete**
 * provider — [onAddressQueryChanged] is the hook; no geocoder ships in the nav-chrome PR.
 */
@Composable
fun AddStopScreen(
    state: AddStopFormState,
    onChange: (AddStopFormState) -> Unit,
    onAddAnotherCargo: () -> Unit,
    onSave: () -> Unit,
    onAddressQueryChanged: ((String) -> Unit)? = null,
) {
    TruckieField(
        value = state.address,
        onValueChange = { value ->
            onChange(state.copy(address = value))
            onAddressQueryChanged?.invoke(value)
        },
        label = stringResource(R.string.hint_address),
    )
    if (state.cargoEnabled) {
        Spacer(modifier = Modifier.height(16.dp))
        TruckieBody(text = stringResource(R.string.cargo_at_stop))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OptimizeModeRadio(
                label = stringResource(R.string.pick_up),
                selected = state.cargoAction == CargoAction.PICK_UP,
                onClick = { onChange(state.copy(cargoAction = CargoAction.PICK_UP)) },
            )
            OptimizeModeRadio(
                label = stringResource(R.string.drop_off),
                selected = state.cargoAction == CargoAction.DROP_OFF,
                onClick = { onChange(state.copy(cargoAction = CargoAction.DROP_OFF)) },
            )
        }
        TruckieField(
            state.cargoDescription,
            { onChange(state.copy(cargoDescription = it)) },
            stringResource(R.string.hint_cargo_description),
        )
        TruckieField(
            state.cargoHeightM,
            { onChange(state.copy(cargoHeightM = it)) },
            stringResource(R.string.hint_height_m),
            KeyboardType.Decimal,
        )
        TruckieField(
            state.cargoWidthM,
            { onChange(state.copy(cargoWidthM = it)) },
            stringResource(R.string.hint_width_m),
            KeyboardType.Decimal,
        )
        TruckieField(
            state.cargoLengthM,
            { onChange(state.copy(cargoLengthM = it)) },
            stringResource(R.string.hint_length_m),
            KeyboardType.Decimal,
        )
        TruckieField(
            state.cargoMassT,
            { onChange(state.copy(cargoMassT = it)) },
            stringResource(R.string.hint_mass_t),
            KeyboardType.Decimal,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TruckiePrimaryButton(
            text = stringResource(R.string.add_another_cargo),
            onClick = onAddAnotherCargo,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.pendingCargoLines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            TruckieBody(text = state.pendingCargoLines.joinToString("\n"))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.save_stop),
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun BlockedRouteScreen(
    reason: String,
    onEditStops: () -> Unit,
    onEditVehicle: () -> Unit,
) {
    TruckieBody(text = reason)
    Spacer(modifier = Modifier.height(24.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.edit_stops),
        onClick = onEditStops,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.edit_vehicle),
        onClick = onEditVehicle,
        modifier = Modifier.fillMaxWidth(),
    )
}
