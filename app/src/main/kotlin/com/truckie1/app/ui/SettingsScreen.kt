package com.truckie1.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.truckie1.app.BuildConfig
import com.truckie1.app.R
import com.truckie1.app.settings.InstalledNavigationAppQuery
import com.truckie1.app.settings.NavigationAppPreference
import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.app.settings.NavigationAppStore
import com.truckie1.flags.LocalFeatureFlags

@Composable
fun SettingsScreen(
    bufferM: Double,
    showNavigationAppPicker: Boolean = false,
    navigationAppStore: NavigationAppStore? = null,
    navigationAppQuery: InstalledNavigationAppQuery? = null,
    featureFlags: LocalFeatureFlags? = null,
    showDemoModeSwitch: Boolean = false,
    /** Debug dogfood: show [BuildConfig.VERSION_NAME] so installs of a fix can be confirmed. */
    showAppVersion: Boolean = true,
    appVersionName: String = BuildConfig.VERSION_NAME,
) {
    TruckieBody(text = stringResource(R.string.clearance_buffer_label))
    Spacer(modifier = Modifier.height(8.dp))
    TruckieBody(text = stringResource(R.string.settings_buffer_value, "%.2f".format(bufferM)))
    Spacer(modifier = Modifier.height(16.dp))
    TruckieBody(text = stringResource(R.string.settings_stub_note))
    if (showAppVersion) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_app_version, appVersionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("settings_app_version"),
        )
    }

    if (showDemoModeSwitch && featureFlags != null) {
        Spacer(modifier = Modifier.height(24.dp))
        DemoModeSwitchSection(flags = featureFlags)
    }

    if (showNavigationAppPicker && navigationAppStore != null && navigationAppQuery != null) {
        Spacer(modifier = Modifier.height(24.dp))
        NavigationAppPickerSection(
            store = navigationAppStore,
            query = navigationAppQuery,
        )
    }
}

@Composable
private fun DemoModeSwitchSection(flags: LocalFeatureFlags) {
    var enabled by remember { mutableStateOf(DemoModeCatalog.isEnabled(flags)) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("settings_demo_mode_row"),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_demo_mode_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_demo_mode_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { checked ->
                enabled = checked
                DemoModeCatalog.setEnabled(flags, enabled = checked)
            },
            modifier = Modifier.testTag(DemoModeCatalog.SETTINGS_TEST_TAG),
        )
    }
}

@Composable
private fun NavigationAppPickerSection(
    store: NavigationAppStore,
    query: InstalledNavigationAppQuery,
) {
    val options = remember(query) { NavigationAppPreference.options(query) }
    var selectedPackage by remember { mutableStateOf(store.packageName()) }

    Text(
        text = stringResource(R.string.settings_nav_app_label),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("settings_nav_app_label"),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.settings_nav_app_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Column(modifier = Modifier.testTag("settings_nav_app_options")) {
        options.forEach { option ->
            val label = if (option is NavigationAppSelection.InAppTruckie) {
                stringResource(R.string.settings_nav_app_in_app)
            } else {
                option.label
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedPackage == option.packageName,
                        onClick = {
                            selectedPackage = option.packageName
                            store.setSelection(option)
                        },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 6.dp)
                    .testTag("settings_nav_app_${option.packageName}"),
            ) {
                RadioButton(
                    selected = selectedPackage == option.packageName,
                    onClick = {
                        selectedPackage = option.packageName
                        store.setSelection(option)
                    },
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable {
                            selectedPackage = option.packageName
                            store.setSelection(option)
                        },
                )
            }
        }
    }
}
