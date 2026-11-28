package com.truckie1.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.truckie1.app.TruckieApp
import com.truckie1.app.trips.AddressPackIndex
import com.truckie1.app.trips.CachedAddressPackRepository
import com.truckie1.app.trips.GnafAddressCatalog
import com.truckie1.app.trips.OwnedAddressPack
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.trips.VerifiedAddressCatalog
import com.truckie1.flags.FlagKeys
import kotlinx.coroutines.launch

/**
 * Thin Compose binder for map-home (ATNA-90 / P1-5).
 *
 * Domain mutations live in [MapHomeController]; this file only remembers the
 * controller, derives display path / chrome flags, and wires [MapHomeScreen].
 *
 * Gherkin: features/map_home_orchestration.feature
 */
@Composable
internal fun MapHomeRouteContent(
    app: TruckieApp,
    demoRevision: Int = 0,
    onNavigatePrimary: (TruckiePrimaryDestination) -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(app) {
        MapHomeController(
            routeSession = app.routeSession,
            navigateSession = app.routeNavigateSession,
            polylineSession = app.mapRoutePolylineSession,
            flags = app.featureFlags,
            bridgeLoader = { app.bridgePackRepository.loadActive().bridges },
            activeVehicleHeightM = {
                app.vehicleProfiles.activeRego
                    ?.let { app.vehicleProfiles.findByRego(it)?.heightM }
            },
            clearanceBufferM = { app.clearanceBufferM },
        )
    }
    // Force recomposition when controller mutates (plain vars, not Snapshot state).
    var uiTick by remember { mutableStateOf(0) }
    fun bump() {
        uiTick += 1
    }

    LaunchedEffect(demoRevision) {
        controller.onDemoRevision()
        bump()
    }
    // Read tick so Compose tracks recompositions after mutations.
    @Suppress("UNUSED_EXPRESSION")
    uiTick

    val showBridgeMarkers = BridgeMarkersCatalog.shouldShow(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    val showStopMarkers = MapStopMarkersCatalog.shouldShow(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    val showRoutePolyline = MapRoutePolylineCatalog.shouldShow(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    val prioritiseStreetLabels = MapStreetLabelPolicy.shouldApply(
        flags = app.featureFlags,
        mapHomeEnabled = true,
    )
    val activeVehicle = remember(app.vehicleProfiles.activeRego, demoRevision, uiTick) {
        app.vehicleProfiles.activeRego?.let { app.vehicleProfiles.findByRego(it) }
    }
    val routingScope = rememberCoroutineScope()
    val displayPathPoints = MapRoutePolylineState.rememberDisplayPoints(
        flags = app.featureFlags,
        mapHomeEnabled = true,
        route = controller.route,
        vehicle = activeVehicle,
        bufferM = app.clearanceBufferM,
        pathRouter = app.pathRouter(),
        composer = app.routeOptimizer,
        session = app.mapRoutePolylineSession,
        navigatePathPoints = controller.navigatePathPoints,
        showRoutePolyline = showRoutePolyline,
        revision = controller.polylineRevision + demoRevision + uiTick,
        currentLocation = controller.currentLocation,
    )
    val bridges = remember(
        showBridgeMarkers,
        app.bridgePackRepository,
        app.featureFlags,
        activeVehicle?.heightM,
        app.clearanceBufferM,
        demoRevision,
        uiTick,
    ) {
        controller.visibleBridges(showBridgeMarkers)
    }
    val nextDestinationAddress = remember(
        controller.route,
        controller.navigatePathPoints,
        app.featureFlags.isEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY),
        uiTick,
    ) {
        controller.nextDestinationAddress()
    }

    MapHomeScreen(
        state = controller.uiState(displayPathPoints = displayPathPoints),
        onSheetValueChange = {
            controller.setSheetValue(it)
            bump()
        },
        onCreateRoute = { input ->
            controller.createRoute(input)
            bump()
        },
        onAddStop = { addressLabel, kind, suggestion ->
            controller.addStop(addressLabel, kind, suggestion)
            bump()
        },
        onNavigateRoute = {
            val selection = app.navigationAppStore.selection { pkg ->
                app.navigationAppQuery.list().firstOrNull { it.packageName == pkg }?.label
            }
            routingScope.launch {
                when (val result = controller.navigate(activeVehicle, selection)) {
                    is RouteNavigateSession.Result.Ready -> {
                        bump()
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
            }
        },
        onNavigatePrimary = onNavigatePrimary,
        canNavigatePrimary = { destination ->
            NavChromeCatalog.canNavigate(destination, app.featureFlags)
        },
        autocomplete = remember(app, uiTick) {
            val addressPackIndex = if (OwnedAddressPack.shouldConsultPack(app.featureFlags)) {
                runCatching {
                    AddressPackIndex.fromPack(
                        CachedAddressPackRepository(
                            context = context,
                            downloader = OwnedAddressPack.downloaderFor(app.featureFlags),
                        ).loadActive(),
                    )
                }.getOrNull()
            } else {
                null
            }
            val gnafIndex = if (
                addressPackIndex == null &&
                GnafAddressCatalog.isEnabled(app.featureFlags)
            ) {
                runCatching {
                    context.assets.open(GnafAddressCatalog.ASSET_PATH)
                        .bufferedReader()
                        .use { GnafAddressCatalog.indexFromJson(it.readText()) }
                }.getOrNull()
            } else {
                null
            }
            VerifiedAddressCatalog.autocompleteFor(
                flags = app.featureFlags,
                gnafIndex = gnafIndex,
                addressPackIndex = addressPackIndex,
            )
        },
        requireVerifiedAddress = VerifiedAddressCatalog.requiresVerifiedSuggestion(app.featureFlags),
        requireStartEnd = controller.requireStartEnd(),
        showNavigateAction = controller.showNavigateAction(),
        showLocationPuck = MapLocationPuck.isEnabled(app.featureFlags),
        locationFollowEnabled = MapLocationFollow.isEnabled(app.featureFlags),
        headingUpEnabled = controller.headingUpEnabled,
        showOrientationLock = controller.showOrientationLock(),
        onOrientationLockChange = { locked ->
            controller.setHeadingUp(locked)
            bump()
        },
        onLocationFixChange = { fix ->
            controller.onLocationFix(fix)
            bump()
        },
        bridges = bridges,
        showBridgeMarkers = showBridgeMarkers,
        showStopMarkers = showStopMarkers,
        prioritiseStreetLabels = prioritiseStreetLabels,
        nextDestinationAddress = nextDestinationAddress,
        modifier = Modifier.fillMaxSize(),
    )
}
