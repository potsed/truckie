package com.truckie1.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * High-contrast Truckie palette (PR #6 intent): navy chrome/text, sky surfaces,
 * white labels on filled primary buttons.
 */
object TruckiePalette {
    val Navy = Color(0xFF0B1F33)
    val Amber = Color(0xFFE0A100)
    val Sky = Color(0xFFD8E6F2)
    val OnPrimary = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
}

object TruckieColorScheme {
    fun light(): ColorScheme = lightColorScheme(
        primary = TruckiePalette.Navy,
        onPrimary = TruckiePalette.OnPrimary,
        primaryContainer = TruckiePalette.Navy,
        onPrimaryContainer = TruckiePalette.OnPrimary,
        secondary = TruckiePalette.Amber,
        onSecondary = TruckiePalette.Navy,
        background = TruckiePalette.Sky,
        onBackground = TruckiePalette.Navy,
        surface = TruckiePalette.Surface,
        onSurface = TruckiePalette.Navy,
        surfaceVariant = TruckiePalette.Sky,
        onSurfaceVariant = TruckiePalette.Navy,
    )
}

/**
 * Catalog of Compose-hosted screen Activities and obsolete XML layouts removed
 * in the migration. NavHost-only (ATNA-74): [MainActivity] is the sole host.
 */
object ComposeScreenCatalog {
    val activitySimpleNames: List<String> = listOf(
        "MainActivity",
    )

    val deletedLayoutResourceNames: List<String> = listOf(
        "activity_main",
        "activity_vehicle_list",
        "activity_vehicle_edit",
        "activity_trip_stops",
        "activity_add_stop",
        "activity_blocked_route",
        "item_vehicle_row",
        "item_trip_stop_row",
    )

    /** Theme.Truckie is NoActionBar; content uses WindowInsets padding. */
    const val usesActionBar: Boolean = false
    const val usesEdgeToEdgeInsets: Boolean = true
}

@Composable
fun TruckieTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Product UI is the light high-contrast scheme regardless of system dark mode
    // until a dedicated dark palette ships.
    val colorScheme = TruckieColorScheme.light()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
