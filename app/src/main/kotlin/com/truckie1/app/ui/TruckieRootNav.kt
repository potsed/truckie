package com.truckie1.app.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.profiles.HomeVehicleSummary
import com.truckie1.app.profiles.VehicleFormParser
import com.truckie1.app.trips.HomeTripSummary
import com.truckie1.app.trips.OptimizeForVehicleGate
import com.truckie1.app.trips.StopFormParser
import com.truckie1.app.AndroidAutoGate
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.OptimizeResult
import com.truckie1.trips.CargoAction
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop

/**
 * Global fixed chrome shell: TopAppBar + optional bottom NavigationBar are composed
 * **once** around a NavHost. Destination content scrolls/swaps in the middle; bars
 * never remount when switching Home / Trip / Vehicles / Settings.
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
    val graphStart = remember(Unit) { primaryStartDestination(startRoute) }

    LaunchedEffect(startRoute) {
        openStartRoute(navController, startRoute)
    }

    BackHandler(enabled = canNavigateUp) {
        navController.popBackStack()
    }

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
                    onNavigate = { destination ->
                        navigatePrimary(navController, app, activity, destination)
                    },
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
                        onNavigatePrimary = { destination ->
                            navigatePrimary(navController, app, activity, destination)
                        },
                    )
                } else {
                    RootScrollBody {
                        HomeRoute(
                            app = app,
                            onPlanTrip = {
                                navigatePrimary(
                                    navController,
                                    app,
                                    activity,
                                    TruckiePrimaryDestination.PLAN_TRIP,
                                )
                            },
                            onVehicleProfiles = {
                                navigatePrimary(
                                    navController,
                                    app,
                                    activity,
                                    TruckiePrimaryDestination.VEHICLES,
                                )
                            },
                            onSettings = {
                                navigatePrimary(
                                    navController,
                                    app,
                                    activity,
                                    TruckiePrimaryDestination.SETTINGS,
                                )
                            },
                        )
                    }
                }
            }
            composable(TruckieRoutes.PLAN_TRIP) {
                RootFillBody {
                    TripStopsRoute(
                        app = app,
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

private fun primaryStartDestination(startRoute: String): String {
    return when (TruckieRoutes.baseRoute(startRoute)) {
        TruckieRoutes.PLAN_TRIP,
        TruckieRoutes.ADD_STOP,
        TruckieRoutes.BLOCKED_ROUTE,
        -> TruckieRoutes.PLAN_TRIP
        TruckieRoutes.VEHICLES,
        TruckieRoutes.VEHICLE_EDIT,
        -> TruckieRoutes.VEHICLES
        TruckieRoutes.SETTINGS -> TruckieRoutes.SETTINGS
        else -> TruckieRoutes.HOME
    }
}

private fun openStartRoute(navController: NavHostController, startRoute: String) {
    val base = TruckieRoutes.baseRoute(startRoute) ?: return
    if (base in TruckieRoutes.primaryRoutes) {
        if (TruckieRoutes.baseRoute(navController.currentDestination?.route) == base) return
        navController.navigate(base) {
            launchSingleTop = true
        }
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

private fun navigatePrimary(
    navController: NavHostController,
    app: TruckieApp,
    activity: ComponentActivity,
    destination: TruckiePrimaryDestination,
) {
    if (!NavChromeCatalog.canNavigate(destination, app.featureFlags)) {
        val flag = NavChromeCatalog.requiredFlag(destination) ?: return
        Toast.makeText(activity, activity.getString(R.string.flag_off, flag), Toast.LENGTH_SHORT).show()
        return
    }
    val route = NavChromeCatalog.routeFor(destination)
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
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
 */
@Composable
private fun MapHomeRoute(
    app: TruckieApp,
    onNavigatePrimary: (TruckiePrimaryDestination) -> Unit,
) {
    val context = LocalContext.current
    var sheetValue by remember { mutableStateOf(RouteSheetValue.Peek) }
    var route by remember { mutableStateOf(app.routeSession.activeRoute()) }
    var navigatePathPoints by remember {
        mutableStateOf(app.routeNavigateSession.activePath()?.points.orEmpty())
    }
    val summary = remember(route) {
        route?.let { com.truckie1.trips.RouteSummary.strip(it) }
            ?: com.truckie1.trips.RouteSummary.emptyStrip()
    }
    val requireStartEnd = com.truckie1.app.trips.RouteStartEndCatalog
        .requiresEndpointsOnCreate(app.featureFlags)
    val showNavigate = com.truckie1.app.trips.RouteNavigateCatalog
        .showsNavigateAction(app.featureFlags)
    val showBridgeMarkers = BridgeMarkersCatalog.shouldShow(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    // TEMPORARY / DEV: Jump control requires Demo mode + bridge markers.
    val showJumpToSampleBridges = JumpToSampleBridges.shouldShow(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    var headingUpEnabled by remember {
        mutableStateOf(MapHeadingUpCatalog.isEnabled(app.featureFlags))
    }
    val showOrientationLock = MapHeadingUpCatalog.shouldShowOnMap(app.featureFlags)
    val activeVehicleHeightM = remember(app.vehicleProfiles.activeRego) {
        app.vehicleProfiles.activeRego
            ?.let { app.vehicleProfiles.findByRego(it)?.heightM }
    }
    val bridges = remember(
        showBridgeMarkers,
        app.bridgePackRepository,
        app.featureFlags,
        activeVehicleHeightM,
        app.clearanceBufferM,
    ) {
        if (showBridgeMarkers) {
            val visible = QldOpenClearances.visibleBridges(
                bridges = app.bridgePackRepository.loadActive().bridges,
                flags = app.featureFlags,
            )
            // W5-8 only for verified low / QR underbridges (ATNA-45).
            val lowOnly = LowBridgeW58.bridgesForW58(visible, app.featureFlags)
            // Under-restricting structures only when passage flag on (ATNA-48).
            val underRelevant = BridgePassageFilter.bridgesRestrictingUnder(
                bridges = lowOnly,
                flags = app.featureFlags,
            )
            // Conflicts only when active vehicle has usable height (ATNA-47).
            VehicleHeightBridgeFilter.bridgesAffectingVehicle(
                bridges = underRelevant,
                flags = app.featureFlags,
                vehicleHeightM = activeVehicleHeightM,
                bufferM = app.clearanceBufferM,
            )
        } else {
            emptyList()
        }
    }

    MapHomeScreen(
        state = MapHomeUiState(
            route = route,
            sheetValue = sheetValue,
            summaryStrip = summary,
            navigatePathPoints = navigatePathPoints,
        ),
        onSheetValueChange = { sheetValue = it },
        onCreateRoute = { input ->
            val start = if (requireStartEnd) {
                when (
                    val built = com.truckie1.app.trips.VerifiedRouteEndpointBuilder.build(input.start)
                ) {
                    is com.truckie1.app.trips.VerifiedRouteEndpointBuilder.Result.Ok -> built.endpoint
                    is com.truckie1.app.trips.VerifiedRouteEndpointBuilder.Result.Err -> return@MapHomeScreen
                }
            } else {
                null
            }
            val end = if (requireStartEnd) {
                when (
                    val built = com.truckie1.app.trips.VerifiedRouteEndpointBuilder.build(input.end)
                ) {
                    is com.truckie1.app.trips.VerifiedRouteEndpointBuilder.Result.Ok -> built.endpoint
                    is com.truckie1.app.trips.VerifiedRouteEndpointBuilder.Result.Err -> return@MapHomeScreen
                }
            } else {
                null
            }
            route = app.routeSession.createRoute(
                name = input.name,
                dateChoice = input.dateChoice,
                customDate = input.customDate,
                start = start,
                end = end,
            )
            app.routeNavigateSession.clear()
            navigatePathPoints = emptyList()
            sheetValue = RouteSheetValue.Half
        },
        onAddStop = { addressLabel, kind, suggestion ->
            val requireVerified = com.truckie1.app.trips.VerifiedAddressCatalog
                .requiresVerifiedSuggestion(app.featureFlags)
            when (
                val built = com.truckie1.app.trips.VerifiedRouteStopBuilder.build(
                    id = newStopId(),
                    kind = kind,
                    suggestion = suggestion,
                    freeTextLabel = addressLabel,
                    requireVerified = requireVerified,
                )
            ) {
                is com.truckie1.app.trips.VerifiedRouteStopBuilder.Result.Ok -> {
                    app.routeSession.addStop(built.stop)
                    route = app.routeSession.activeRoute()
                }
                is com.truckie1.app.trips.VerifiedRouteStopBuilder.Result.Err -> {
                    // Verified mode: ignore free-text-only / incomplete attempts.
                }
            }
        },
        onNavigateRoute = {
            val selection = app.navigationAppStore.selection { pkg ->
                app.navigationAppQuery.list().firstOrNull { it.packageName == pkg }?.label
            }
            when (
                val result = app.routeNavigateSession.start(
                    flags = app.featureFlags,
                    route = route,
                    selection = selection,
                )
            ) {
                is com.truckie1.app.trips.RouteNavigateSession.Result.Ready -> {
                    navigatePathPoints = result.path.points
                    result.externalHandoff?.let { handoff ->
                        val intent = com.truckie1.app.settings.ExternalNavigationHandoff.buildIntent(
                            packageName = handoff.packageName,
                            latitude = handoff.destination.latitude,
                            longitude = handoff.destination.longitude,
                        )
                        runCatching { context.startActivity(intent) }
                    }
                }
                else -> Unit
            }
        },
        onNavigatePrimary = onNavigatePrimary,
        canNavigatePrimary = { destination ->
            NavChromeCatalog.canNavigate(destination, app.featureFlags)
        },
        autocomplete = com.truckie1.app.trips.VerifiedAddressCatalog.autocompleteFor(
            app.featureFlags,
        ),
        requireVerifiedAddress = com.truckie1.app.trips.VerifiedAddressCatalog
            .requiresVerifiedSuggestion(app.featureFlags),
        requireStartEnd = requireStartEnd,
        showNavigateAction = showNavigate,
        showLocationPuck = MapLocationPuck.isEnabled(app.featureFlags),
        locationFollowEnabled = MapLocationFollow.isEnabled(app.featureFlags),
        headingUpEnabled = headingUpEnabled,
        showOrientationLock = showOrientationLock,
        onOrientationLockChange = { locked ->
            headingUpEnabled = locked
            MapHeadingUpCatalog.setEnabled(app.featureFlags, enabled = locked)
        },
        bridges = bridges,
        showBridgeMarkers = showBridgeMarkers,
        showJumpToSampleBridges = showJumpToSampleBridges,
        modifier = Modifier.fillMaxSize(),
    )
}

private fun bindHomeState(app: TruckieApp): HomeUiState {
    val activeSummary = HomeVehicleSummary.text(app.featureFlags, app.vehicleProfiles)
    val active = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
    val clearanceLine = if (activeSummary == null) {
        null
    } else {
        HomeTripSummary.clearanceLine(
            flags = app.featureFlags,
            session = app.tripSession,
            vehicleHeightM = active?.heightM,
            bufferM = app.clearanceBufferM,
        ) ?: app.getString(R.string.set_vehicle_for_clearance)
    }
    val autoStatus = when {
        AndroidAutoGate.isEnabled(app.featureFlags) -> "Android Auto: available"
        !AndroidAutoGate.isSupportedApi() -> app.getString(R.string.auto_unavailable)
        else -> "Android Auto: flag ${FlagKeys.ANDROID_AUTO} off"
    }
    return HomeUiState(
        activeVehicle = activeSummary,
        clearanceLine = clearanceLine,
        tripSummary = HomeTripSummary.text(app.featureFlags, app.tripSession),
        autoStatus = autoStatus,
        showPlanTrip = app.featureFlags.isEnabled(FlagKeys.MULTI_STOP),
        showVehicleProfiles = app.featureFlags.isEnabled(FlagKeys.VEHICLE_PROFILES),
    )
}

@Composable
private fun ColumnScope.TripStopsRoute(
    app: TruckieApp,
    onAddStop: () -> Unit,
    onBlocked: (String) -> Unit,
) {
    val context = LocalContext.current
    var stops by remember { mutableStateOf(app.tripSession.stops()) }
    var clearanceSummary by remember { mutableStateOf("") }
    var showOptimize by remember {
        mutableStateOf(OptimizeForVehicleGate.isAvailable(app.featureFlags))
    }
    var optimizeMode by remember { mutableStateOf(OptimizeMode.FASTEST) }
    var optimizeStatus by remember { mutableStateOf<String?>(null) }

    fun bindList() {
        val current = app.tripSession.stops()
        stops = current
        clearanceSummary = tripClearanceText(app, current)
        showOptimize = OptimizeForVehicleGate.isAvailable(app.featureFlags)
    }

    LaunchedEffect(Unit) { bindList() }

    TripStopsScreen(
        stops = stops,
        clearanceSummary = clearanceSummary,
        showOptimize = showOptimize,
        optimizeMode = optimizeMode,
        optimizeStatus = optimizeStatus,
        onAddStop = onAddStop,
        onMoveUp = { index ->
            if (index > 0) {
                app.tripSession.moveStop(index, index - 1)
                bindList()
            }
        },
        onMoveDown = { index ->
            if (index < stops.lastIndex) {
                app.tripSession.moveStop(index, index + 1)
                bindList()
            }
        },
        onDelete = { stop ->
            app.tripSession.removeStop(stop.id)
            bindList()
        },
        onModeChange = { optimizeMode = it },
        onOptimize = {
            if (!OptimizeForVehicleGate.isAvailable(app.featureFlags)) {
                Toast.makeText(
                    context,
                    context.getString(R.string.flag_off, FlagKeys.ROUTE_OPTIMIZE),
                    Toast.LENGTH_SHORT,
                ).show()
                return@TripStopsScreen
            }
            val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
            if (vehicle == null) {
                Toast.makeText(context, context.getString(R.string.optimize_need_vehicle), Toast.LENGTH_SHORT).show()
                return@TripStopsScreen
            }
            val currentStops = app.tripSession.stops()
            if (currentStops.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.optimize_need_stops), Toast.LENGTH_SHORT).show()
                return@TripStopsScreen
            }
            when (
                val result = app.routeOptimizer.optimize(
                    stops = currentStops,
                    vehicle = vehicle,
                    bufferM = app.clearanceBufferM,
                    mode = optimizeMode,
                )
            ) {
                is OptimizeResult.Ok -> {
                    app.tripSession.replaceStops(result.orderedStops)
                    app.tripSession.lastOptimizeMode = result.mode
                    optimizeStatus = context.getString(
                        R.string.optimize_success,
                        result.mode.name.lowercase().replaceFirstChar { it.titlecase() },
                        result.route.durationSeconds,
                    )
                    bindList()
                }
                is OptimizeResult.Blocked -> onBlocked(result.reason)
            }
        },
    )
}

private fun tripClearanceText(app: TruckieApp, stops: List<TripStop>): String {
    val vehicle = app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
    if (vehicle == null) return app.getString(R.string.set_vehicle_for_clearance)
    val effective = if (app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)) {
        ClearanceCalculator.maxEffectiveHeightAlongTripM(
            vehicleHeightM = vehicle.heightM,
            stops = stops,
            bufferM = app.clearanceBufferM,
        )
    } else {
        ClearanceCalculator.effectiveHeightM(
            vehicleHeightM = vehicle.heightM,
            cargoHeightsM = emptyList(),
            bufferM = app.clearanceBufferM,
        )
    }
    return app.getString(R.string.trip_eff_clearance, "%.2f".format(effective))
}

@Composable
private fun ColumnScope.VehicleListRoute(
    app: TruckieApp,
    onAdd: () -> Unit,
    onEdit: (Vehicle) -> Unit,
) {
    val context = LocalContext.current
    var vehicles by remember { mutableStateOf(app.vehicleProfiles.all()) }
    var activeRego by remember { mutableStateOf(app.vehicleProfiles.activeRego) }

    fun bindList() {
        vehicles = app.vehicleProfiles.all()
        activeRego = app.vehicleProfiles.activeRego
    }

    LaunchedEffect(Unit) { bindList() }

    VehicleListScreen(
        vehicles = vehicles,
        activeRego = activeRego,
        onAdd = onAdd,
        onSelect = { vehicle ->
            app.vehicleProfiles.setActive(vehicle.rego)
            bindList()
            Toast.makeText(
                context,
                context.getString(R.string.active_set, vehicle.rego),
                Toast.LENGTH_SHORT,
            ).show()
        },
        onEdit = onEdit,
    )
}

@Composable
private fun AddStopRoute(
    app: TruckieApp,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val pendingCargo = remember { mutableListOf<StopFormParser.CargoInput>() }
    var form by remember {
        mutableStateOf(
            AddStopFormState(cargoEnabled = app.featureFlags.isEnabled(FlagKeys.STOP_CARGO)),
        )
    }
    val cargoEnabled = form.cargoEnabled

    AddStopScreen(
        state = form,
        onChange = { form = it },
        onAddAnotherCargo = {
            if (!cargoEnabled) return@AddStopScreen
            stageCurrentCargoRow(form)?.let { pendingCargo += it }
            form = form.copy(
                cargoDescription = "",
                cargoHeightM = "",
                cargoWidthM = "",
                cargoLengthM = "",
                cargoMassT = "",
                cargoAction = CargoAction.PICK_UP,
                pendingCargoLines = pendingCargo.mapIndexed { index, cargo ->
                    val action = if (cargo.action == CargoAction.DROP_OFF.name) "drop" else "pick"
                    "${index + 1}. $action · ${cargo.description} · ${cargo.heightM}m / ${cargo.massT}t"
                },
            )
        },
        onSave = {
            val cargoItems = pendingCargo.toMutableList()
            if (cargoEnabled) {
                stageCurrentCargoRow(form)?.let { cargoItems += it }
            }
            when (
                val result = StopFormParser.parse(
                    StopFormParser.Input(
                        addressLabel = form.address,
                        cargoEnabled = cargoEnabled,
                        cargoItems = cargoItems,
                    ),
                )
            ) {
                is StopFormParser.Result.Err ->
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                is StopFormParser.Result.Ok -> {
                    app.tripSession.addStop(result.stop)
                    Toast.makeText(
                        context,
                        context.getString(R.string.saved_stop, result.stop.addressLabel),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onSaved()
                }
            }
        },
    )
}

private fun stageCurrentCargoRow(form: AddStopFormState): StopFormParser.CargoInput? {
    val description = form.cargoDescription
    val height = form.cargoHeightM
    val width = form.cargoWidthM
    val length = form.cargoLengthM
    val mass = form.cargoMassT
    if (description.isBlank() && height.isBlank() && width.isBlank() && length.isBlank() && mass.isBlank()) {
        return null
    }
    return StopFormParser.CargoInput(
        action = form.cargoAction.name,
        description = description,
        heightM = height,
        widthM = width,
        lengthM = length,
        massT = mass,
    )
}

@Composable
private fun VehicleEditRoute(
    app: TruckieApp,
    existingRego: String?,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var form by remember {
        val existing = existingRego?.let { app.vehicleProfiles.findByRego(it) }
        mutableStateOf(
            existing?.toEditFormState(regoEnabled = false) ?: VehicleEditFormState(),
        )
    }

    VehicleEditScreen(
        state = form,
        onChange = { form = it },
        onSave = {
            when (
                val result = VehicleFormParser.parse(
                    VehicleFormParser.Input(
                        rego = form.rego,
                        nickname = form.nickname,
                        vehicleType = form.vehicleType,
                        make = form.make,
                        model = form.model,
                        year = form.year,
                        heightM = form.heightM,
                        widthM = form.widthM,
                        lengthM = form.lengthM,
                        massT = form.massT,
                        tareT = form.tareT,
                        gvmT = form.gvmT,
                        gcmT = form.gcmT,
                        axles = form.axles,
                        networkClass = form.networkClass,
                    ),
                )
            ) {
                is VehicleFormParser.Result.Err ->
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                is VehicleFormParser.Result.Ok -> {
                    app.vehicleProfiles.save(result.vehicle)
                    Toast.makeText(
                        context,
                        context.getString(R.string.saved_vehicle, result.vehicle.rego),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onSaved()
                }
            }
        },
    )
}

private fun Vehicle.toEditFormState(regoEnabled: Boolean): VehicleEditFormState = VehicleEditFormState(
    rego = rego,
    nickname = nickname,
    vehicleType = vehicleType,
    make = make,
    model = model,
    year = year.toString(),
    heightM = heightM.toString(),
    widthM = widthM.toString(),
    lengthM = lengthM.toString(),
    massT = massT.toString(),
    tareT = tareT.toString(),
    gvmT = gvmT.toString(),
    gcmT = gcmT.toString(),
    axles = axleCount.toString(),
    networkClass = networkClass.orEmpty(),
    regoEnabled = regoEnabled,
)
