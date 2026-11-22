package com.truckie1.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckie1.app.R

data class HomeUiState(
    val activeVehicle: String?,
    val clearanceLine: String?,
    val tripSummary: String?,
    val autoStatus: String,
    val showPlanTrip: Boolean,
    val showVehicleProfiles: Boolean,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlanTrip: () -> Unit,
    onVehicleProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    if (state.activeVehicle != null) {
        TruckieBody(text = state.activeVehicle)
        if (state.clearanceLine != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TruckieBody(text = state.clearanceLine)
        }
    }
    if (state.tripSummary != null) {
        Spacer(modifier = Modifier.height(8.dp))
        TruckieBody(text = state.tripSummary)
    }
    Spacer(modifier = Modifier.height(24.dp))
    if (state.showPlanTrip) {
        TruckiePrimaryButton(
            text = stringResource(R.string.plan_trip),
            onClick = onPlanTrip,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (state.showVehicleProfiles) {
        TruckiePrimaryButton(
            text = stringResource(R.string.vehicle_profiles),
            onClick = onVehicleProfiles,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    TruckiePrimaryButton(
        text = stringResource(R.string.settings),
        onClick = onSettings,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    TruckieBody(text = state.autoStatus)
}
