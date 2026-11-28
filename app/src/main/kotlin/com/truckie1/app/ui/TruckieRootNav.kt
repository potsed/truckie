package com.truckie1.app.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.trips.TripStop

/**
 * Global fixed chrome shell: TopAppBar + optional bottom NavigationBar are composed
 * **once** around a NavHost. Destination content scrolls/swaps in the middle; bars
 * never remount when switching Home / Trip / Vehicles / Settings.
 *
 * Destination controllers: [RootNavDestinations] / [MapHomeRouteContent] (ATNA-90).
 * Sole host for all primary screens — legacy Activities deleted (ATNA-74).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckieRootApp(
    app: TruckieApp,
    activity: ComponentActivity,
    startRoute: String = TruckieRoutes.HOME,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val baseRoute = TruckieRoutes.baseRoute(currentRoute) ?: TruckieRoutes.HOME
    val selected = NavChromeCatalog.destinationForRoute(baseRoute)
    val mapHomeEnabled = MapHomeCatalog.usesMapHome(app.featureFlags)
    val showGlobalChrome = NavChromeCatalog.showGlobalChromeForRoute(baseRoute, mapHomeEnabled)
    val showBottomBar = NavChromeCatalog.showBottomBarForRoute(baseRoute, mapHomeEnabled)
    val showTopBar = NavChromeCatalog.showTopBarForRoute(baseRoute, mapHomeEnabled)
    val title = rootTitle(baseRoute)
    val canNavigateUp = baseRoute !in TruckieRoutes.primaryRoutes
    val graphStart = remember(Unit) { PrimaryNavigation.graphStartDestination(startRoute) }
    // ATNA-75: bump when Demo mode toggles so UI rebinds the swapped data source.
    var demoRevision by remember { mutableIntStateOf(0) }
    var demoEnabled by remember {
        mutableStateOf(DemoModeCatalog.isEnabled(app.featureFlags))
    }

    LaunchedEffect(startRoute) {
        openStartRoute(navController, startRoute, mapHomeEnabled)
    }

    fun goPrimary(destination: TruckiePrimaryDestination) {
        if (!NavChromeCatalog.canNavigate(destination, app.featureFlags)) {
            val flag = NavChromeCatalog.requiredFlag(destination) ?: return
            Toast.makeText(activity, activity.getString(R.string.flag_off, flag), Toast.LENGTH_SHORT)
                .show()
            return
        }
        PrimaryNavigation.navigatePrimary(
            navController = navController,
            destination = destination,
            mapHomeEnabled = mapHomeEnabled,
            currentRoute = baseRoute,
            startRoute = graphStart,
        )
    }

    BackHandler(enabled = canNavigateUp) {
        navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    navigationIcon = {
                        if (canNavigateUp) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.nav_back),
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
            }
        },
        bottomBar = {
            if (showBottomBar && selected != null) {
                TruckieRootBottomBar(
                    selected = selected,
                    onNavigate = { destination -> goPrimary(destination) },
                )
            }
        },
    ) { innerPadding ->
        val hostModifier = if (showGlobalChrome) {
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        } else {
            // Map-first home: full-bleed under system bars; no chrome inset.
            Modifier.fillMaxSize()
        }
        NavHost(
            navController = navController,
            startDestination = graphStart,
            modifier = hostModifier,
        ) {
            composable(TruckieRoutes.HOME) {
                if (mapHomeEnabled) {
                    MapHomeRoute(
                        app = app,
                        demoRevision = demoRevision,
                        onNavigatePrimary = { destination -> goPrimary(destination) },
                    )
                } else {
                    RootScrollBody {
                        HomeRoute(
                            app = app,
                            onPlanTrip = { goPrimary(TruckiePrimaryDestination.PLAN_TRIP) },
                            onVehicleProfiles = { goPrimary(TruckiePrimaryDestination.VEHICLES) },
                            onSettings = { goPrimary(TruckiePrimaryDestination.SETTINGS) },
                        )
                    }
                }
            }
            composable(TruckieRoutes.PLAN_TRIP) {
                RootFillBody {
                    TripStopsRoute(
                        app = app,
                        demoRevision = demoRevision,
                        onAddStop = { navController.navigate(TruckieRoutes.ADD_STOP) },
                        onBlocked = { reason ->
                            navController.navigate(TruckieRoutes.blockedRoute(reason))
                        },
                    )
                }
            }
            composable(TruckieRoutes.VEHICLES) {
                RootFillBody {
                    VehicleListRoute(
                        app = app,
                        demoRevision = demoRevision,
                        onAdd = { navController.navigate(TruckieRoutes.vehicleEdit()) },
                        onEdit = { vehicle ->
                            navController.navigate(TruckieRoutes.vehicleEdit(vehicle.rego))
                        },
                    )
                }
            }
            composable(TruckieRoutes.SETTINGS) {
                RootScrollBody {
                    SettingsScreen(
                        bufferM = app.clearanceBufferM,
                        showNavigationAppPicker = com.truckie1.app.settings.NavigationAppPreference
                            .showsInSettings(app.featureFlags),
                        navigationAppStore = app.navigationAppStore,
                        navigationAppQuery = app.navigationAppQuery,
                        featureFlags = app.featureFlags,
                        showDemoModeSwitch = DemoModeCatalog.showsInSettings,
                        onDemoModeChanged = { enabled ->
                            app.demoModeController.setEnabled(enabled)
                            demoEnabled = DemoModeCatalog.isEnabled(app.featureFlags)
                            demoRevision = app.demoModeController.revision()
                        },
                    )
                }
            }
            composable(TruckieRoutes.ADD_STOP) {
                RootScrollBody {
                    AddStopRoute(
                        app = app,
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            composable(TruckieRoutes.VEHICLE_EDIT) {
                RootScrollBody {
                    VehicleEditRoute(
                        app = app,
                        existingRego = null,
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            composable(
                route = TruckieRoutes.VEHICLE_EDIT_WITH_REGO,
                arguments = listOf(
                    navArgument(TruckieRoutes.ARG_REGO) { type = NavType.StringType },
                ),
            ) { entry ->
                val rego = entry.arguments?.getString(TruckieRoutes.ARG_REGO)
                RootScrollBody {
                    VehicleEditRoute(
                        app = app,
                        existingRego = rego,
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            composable(
                route = TruckieRoutes.BLOCKED_ROUTE_WITH_REASON,
                arguments = listOf(
                    navArgument(TruckieRoutes.ARG_REASON) { type = NavType.StringType },
                ),
            ) { entry ->
                val reason = entry.arguments?.getString(TruckieRoutes.ARG_REASON)
                    ?.let { android.net.Uri.decode(it) }
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.blocked_route_default_reason)
                RootScrollBody {
                    BlockedRouteScreen(
                        reason = reason,
                        onEditStops = { navController.popBackStack() },
                        onEditVehicle = {
                            navController.popBackStack()
                            if (app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES)) {
                                navController.navigate(TruckieRoutes.VEHICLES) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    if (demoEnabled) {
        DemoModePill(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .zIndex(10f),
        )
    }
    } // Box — DEMO MODE pill floats above chrome
}

@Composable
private fun rootTitle(baseRoute: String): String = when (baseRoute) {
    TruckieRoutes.HOME -> stringResource(R.string.home_title)
    TruckieRoutes.PLAN_TRIP -> stringResource(R.string.trip_stops)
    TruckieRoutes.VEHICLES -> stringResource(R.string.vehicle_profiles)
    TruckieRoutes.SETTINGS -> stringResource(R.string.settings)
    TruckieRoutes.ADD_STOP -> stringResource(R.string.add_stop_title)
    TruckieRoutes.VEHICLE_EDIT -> stringResource(R.string.edit_profile)
    TruckieRoutes.BLOCKED_ROUTE -> stringResource(R.string.blocked_route_title)
    else -> stringResource(R.string.app_name)
}

@Composable
private fun TruckieRootBottomBar(
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
                        imageVector = destination.rootIcon(),
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

private fun TruckiePrimaryDestination.rootIcon(): ImageVector = when (this) {
    TruckiePrimaryDestination.HOME -> Icons.Filled.Home
    TruckiePrimaryDestination.PLAN_TRIP -> Icons.Filled.Place
    TruckiePrimaryDestination.VEHICLES -> Icons.Filled.DirectionsCar
    TruckiePrimaryDestination.SETTINGS -> Icons.Filled.Settings
}

private fun openStartRoute(
    navController: NavHostController,
    startRoute: String,
    mapHomeEnabled: Boolean,
) {
    val base = TruckieRoutes.baseRoute(startRoute) ?: return
    if (base in TruckieRoutes.primaryRoutes) {
        if (TruckieRoutes.baseRoute(navController.currentDestination?.route) == base) return
        // Deep-link / EXTRA_ROUTE into a primary tab — still one policy object.
        PrimaryNavigation.navigatePrimary(
            navController = navController,
            destination = NavChromeCatalog.destinationForRoute(base)
                ?: TruckiePrimaryDestination.HOME,
            mapHomeEnabled = mapHomeEnabled,
            currentRoute = navController.currentDestination?.route,
            startRoute = PrimaryNavigation.graphStartDestination(startRoute),
        )
        return
    }
    val parent = when (base) {
        TruckieRoutes.ADD_STOP,
        TruckieRoutes.BLOCKED_ROUTE,
        -> TruckieRoutes.PLAN_TRIP
        TruckieRoutes.VEHICLE_EDIT -> TruckieRoutes.VEHICLES
        else -> TruckieRoutes.HOME
    }
    if (TruckieRoutes.baseRoute(navController.currentDestination?.route) != parent) {
        navController.navigate(parent) { launchSingleTop = true }
    }
    navController.navigate(startRoute) { launchSingleTop = true }
}

@Composable
private fun RootScrollBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun RootFillBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun HomeRoute(
    app: TruckieApp,
    onPlanTrip: () -> Unit,
    onVehicleProfiles: () -> Unit,
    onSettings: () -> Unit,
) {
    var uiState by remember {
        mutableStateOf(
            HomeUiState(
                activeVehicle = null,
                clearanceLine = null,
                tripSummary = null,
                autoStatus = "",
                showPlanTrip = false,
                showVehicleProfiles = false,
            ),
        )
    }
    LaunchedEffect(Unit) {
        uiState = bindHomeState(app)
    }
    HomeScreen(
        state = uiState,
        onPlanTrip = onPlanTrip,
        onVehicleProfiles = onVehicleProfiles,
        onSettings = onSettings,
    )
}

/**
 * MapLibre map-first home + route/stop drag sheet (`ff_map_home`).
 * Hosted inside the single-activity NavHost with **no** global header/footer chrome.
 * Thin binder — domain in [MapHomeController] (ATNA-90).
 */
@Composable
private fun MapHomeRoute(
    app: TruckieApp,
    demoRevision: Int = 0,
    onNavigatePrimary: (TruckiePrimaryDestination) -> Unit,
) {
    MapHomeRouteContent(
        app = app,
        demoRevision = demoRevision,
        onNavigatePrimary = onNavigatePrimary,
    )
}
