package com.truckie1.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckie1.app.R

/**
 * Renders [LicenseInventory] from the asset SoT (ATNA-116).
 * Do not maintain a separate hardcoded Compose-only licence list.
 */
@Composable
fun LicensesScreen(inventory: LicenseInventory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LicensesCatalog.PAGE_TEST_TAG),
    ) {
        Text(
            text = stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        inventory.entries.forEach { entry ->
            LicenseEntryBlock(entry = entry)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LicenseEntryBlock(entry: LicenseEntry) {
    Text(
        text = entry.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("license_entry_${entry.id}"),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.licenses_license_line, entry.license),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    val copyright = entry.copyright
    if (!copyright.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = copyright,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    val url = entry.url
    if (!url.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    val notes = entry.notes
    if (!notes.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}
