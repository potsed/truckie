package com.truckie1.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckie1.app.R

@Composable
fun SettingsScreen(bufferM: Double) {
    TruckieBody(text = stringResource(R.string.clearance_buffer_label))
    Spacer(modifier = Modifier.height(8.dp))
    TruckieBody(text = stringResource(R.string.settings_buffer_value, "%.2f".format(bufferM)))
    Spacer(modifier = Modifier.height(16.dp))
    TruckieBody(text = stringResource(R.string.settings_stub_note))
}
