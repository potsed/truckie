package com.truckie1.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckie1.app.R
import com.truckie1.profiles.Vehicle

data class VehicleEditFormState(
    val rego: String = "",
    val nickname: String = "",
    val vehicleType: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val heightM: String = "",
    val widthM: String = "",
    val lengthM: String = "",
    val massT: String = "",
    val tareT: String = "",
    val gvmT: String = "",
    val gcmT: String = "",
    val axles: String = "",
    val networkClass: String = "",
    val regoEnabled: Boolean = true,
)

@Composable
fun ColumnScope.VehicleListScreen(
    vehicles: List<Vehicle>,
    activeRego: String?,
    onAdd: () -> Unit,
    onSelect: (Vehicle) -> Unit,
    onEdit: (Vehicle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TruckiePrimaryButton(text = stringResource(R.string.add_vehicle), onClick = onAdd)
    }
    Spacer(modifier = Modifier.height(8.dp))
    TruckieBody(text = stringResource(R.string.tap_row_set_active))
    Spacer(modifier = Modifier.height(12.dp))
    if (vehicles.isEmpty()) {
        TruckieBody(text = stringResource(R.string.no_vehicles_yet))
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
            items(vehicles, key = { it.rego }) { vehicle ->
                VehicleRow(
                    vehicle = vehicle,
                    isActive = vehicle.rego == activeRego,
                    onSelect = { onSelect(vehicle) },
                    onEdit = { onEdit(vehicle) },
                )
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${if (isActive) "★" else " "} ${vehicle.rego}  ${vehicle.vehicleType}  ${"%.1f".format(vehicle.heightM)}m",
                color = TruckiePalette.Navy,
            )
            Text(
                text = "${vehicle.nickname} · ${vehicle.make} ${vehicle.model} (${vehicle.year})",
                color = TruckiePalette.Navy,
            )
        }
        TruckieTextButton(text = stringResource(R.string.edit), onClick = onEdit)
    }
}

@Composable
fun VehicleEditScreen(
    state: VehicleEditFormState,
    onChange: (VehicleEditFormState) -> Unit,
    onSave: () -> Unit,
) {
    TruckieField(
        value = state.rego,
        onValueChange = { onChange(state.copy(rego = it)) },
        label = stringResource(R.string.hint_rego),
        enabled = state.regoEnabled,
        keyboardType = KeyboardType.Ascii,
    )
    TruckieField(state.nickname, { onChange(state.copy(nickname = it)) }, stringResource(R.string.hint_nickname))
    TruckieField(state.vehicleType, { onChange(state.copy(vehicleType = it)) }, stringResource(R.string.hint_vehicle_type))
    TruckieField(state.make, { onChange(state.copy(make = it)) }, stringResource(R.string.hint_make))
    TruckieField(state.model, { onChange(state.copy(model = it)) }, stringResource(R.string.hint_model))
    TruckieField(state.year, { onChange(state.copy(year = it)) }, stringResource(R.string.hint_year), KeyboardType.Number)
    TruckieField(state.heightM, { onChange(state.copy(heightM = it)) }, stringResource(R.string.hint_height_m), KeyboardType.Decimal)
    TruckieField(state.widthM, { onChange(state.copy(widthM = it)) }, stringResource(R.string.hint_width_m), KeyboardType.Decimal)
    TruckieField(state.lengthM, { onChange(state.copy(lengthM = it)) }, stringResource(R.string.hint_length_m), KeyboardType.Decimal)
    TruckieField(state.massT, { onChange(state.copy(massT = it)) }, stringResource(R.string.hint_mass_t), KeyboardType.Decimal)
    TruckieField(state.tareT, { onChange(state.copy(tareT = it)) }, stringResource(R.string.hint_tare_t), KeyboardType.Decimal)
    TruckieField(state.gvmT, { onChange(state.copy(gvmT = it)) }, stringResource(R.string.hint_gvm_t), KeyboardType.Decimal)
    TruckieField(state.gcmT, { onChange(state.copy(gcmT = it)) }, stringResource(R.string.hint_gcm_t), KeyboardType.Decimal)
    TruckieField(state.axles, { onChange(state.copy(axles = it)) }, stringResource(R.string.hint_axles), KeyboardType.Number)
    TruckieField(state.networkClass, { onChange(state.copy(networkClass = it)) }, stringResource(R.string.hint_network_class))
    Spacer(modifier = Modifier.height(16.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.save),
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun TruckieField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TruckiePalette.Navy,
            unfocusedTextColor = TruckiePalette.Navy,
            focusedBorderColor = TruckiePalette.Navy,
            unfocusedBorderColor = TruckiePalette.Navy.copy(alpha = 0.5f),
            focusedLabelColor = TruckiePalette.Navy,
            unfocusedLabelColor = TruckiePalette.Navy,
            cursorColor = TruckiePalette.Navy,
            disabledTextColor = TruckiePalette.Navy.copy(alpha = 0.7f),
        ),
    )
}
