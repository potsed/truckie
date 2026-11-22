package com.truckie1.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** Edge-to-edge + Material3 theme host used by every screen Activity. */
fun ComponentActivity.setTruckieContent(content: @Composable () -> Unit) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(TruckiePalette.Navy.toArgb()),
        navigationBarStyle = SystemBarStyle.light(
            TruckiePalette.Sky.toArgb(),
            TruckiePalette.Sky.toArgb(),
        ),
    )
    setContent {
        TruckieTheme {
            content()
        }
    }
}

/**
 * Chooses Material3 TopAppBar + bottom nav (`ff_nav_chrome`) or legacy padded screen.
 */
@Composable
fun TruckieHostedScreen(
    title: String,
    activitySimpleName: String,
    navChromeEnabled: Boolean,
    onNavigate: (TruckiePrimaryDestination) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val selected = NavChromeCatalog.selectedForActivity(activitySimpleName)
    val showBottomBar = navChromeEnabled && NavChromeCatalog.showBottomBarForActivity(activitySimpleName)
    if (navChromeEnabled) {
        TruckieNavScaffold(
            title = title,
            selected = selected,
            showBottomBar = showBottomBar,
            onNavigate = onNavigate,
            onNavigateUp = onNavigateUp,
            scrollable = scrollable,
            content = content,
        )
    } else {
        TruckieScreen(scrollable = scrollable) {
            TruckieTitle(text = title)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun TruckieScreen(
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val columnModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = columnModifier, content = content)
    }
}

/** Filled navy primary with white label — high contrast (PR #6). */
@Composable
fun TruckiePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text)
    }
}

@Composable
fun TruckieTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun TruckieTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun TruckieBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = modifier,
    )
}
