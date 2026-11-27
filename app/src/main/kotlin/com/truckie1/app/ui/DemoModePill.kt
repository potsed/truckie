package com.truckie1.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.truckie1.flags.FeatureFlags

/**
 * Floating **DEMO MODE** pill — the only product surface that labels demo
 * (ATNA-75). Sheet content / vehicle / route copy stay neutral.
 *
 * Gherkin: features/demo_mode.feature
 */
object DemoModePillCatalog {
    const val LABEL: String = "DEMO MODE"
    const val TEST_TAG: String = "demo_mode_pill"

    fun shouldShow(flags: FeatureFlags): Boolean = DemoModeCatalog.isEnabled(flags)
}

@Composable
fun DemoModePill(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag(DemoModePillCatalog.TEST_TAG)
            .semantics { contentDescription = DemoModePillCatalog.LABEL }
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = DemoModePillCatalog.LABEL,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onError,
        )
    }
}
