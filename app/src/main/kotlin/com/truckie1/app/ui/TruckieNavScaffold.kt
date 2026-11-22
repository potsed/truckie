package com.truckie1.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Legacy per-Activity Scaffold used only when [com.truckie1.flags.FlagKeys.NAV_CHROME]
 * is off. With the flag on, [TruckieRootApp] hosts chrome once around a NavHost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckieNavScaffold(
    title: String,
    selected: TruckiePrimaryDestination?,
    showBottomBar: Boolean,
    onNavigate: (TruckiePrimaryDestination) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    if (onNavigateUp != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(com.truckie1.app.R.string.nav_back),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
        bottomBar = {
            if (showBottomBar && selected != null) {
                TruckieBottomBar(
                    selected = selected,
                    onNavigate = onNavigate,
                )
            }
        },
    ) { innerPadding ->
        TruckieNavBody(
            contentPadding = innerPadding,
            scrollable = scrollable,
            content = content,
        )
    }
}

@Composable
private fun TruckieBottomBar(
    selected: TruckiePrimaryDestination,
    onNavigate: (TruckiePrimaryDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        NavChromeCatalog.primaryDestinations.forEach { destination ->
            val selectedItem = destination == selected
            NavigationBarItem(
                selected = selectedItem,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = stringResource(
                            destination.contentDescriptionRes ?: destination.labelRes,
                        ),
                    )
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                ),
            )
        }
    }
}

@Composable
private fun TruckieNavBody(
    contentPadding: PaddingValues,
    scrollable: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val columnModifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)

    Column(modifier = columnModifier, content = content)
}

private fun TruckiePrimaryDestination.icon(): ImageVector = when (this) {
    TruckiePrimaryDestination.HOME -> Icons.Filled.Home
    TruckiePrimaryDestination.PLAN_TRIP -> Icons.Filled.Place
    TruckiePrimaryDestination.VEHICLES -> Icons.Filled.DirectionsCar
    TruckiePrimaryDestination.SETTINGS -> Icons.Filled.Settings
}
