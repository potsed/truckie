package com.truckie1.app.ui

import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.app.trips.RouteNavigateCatalog
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.trips.RouteStartEndCatalog
import com.truckie1.app.trips.VerifiedAddressCatalog
import com.truckie1.app.trips.VerifiedRouteEndpointBuilder
import com.truckie1.app.trips.VerifiedRouteStopBuilder
import com.truckie1.app.settings.NavigationAppSelection
import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.Route
import com.truckie1.trips.RouteRepository
import com.truckie1.trips.RouteSummary
import com.truckie1.trips.StopKind

/**
 * Map-home domain controller (ATNA-90 / P1-5).
 *
 * Owns create-route, add-stop, Navigate session clears, sheet value, demo rebind,
 * heading-up lock, location approach fix, and bridge visibility — so
 * [MapHomeRoute] stays a thin Compose binder.
 *
 * Gherkin: features/map_home_orchestration.feature
 */
class MapHomeController(
    private val routeSession: RouteRepository,
    private val navigateSession: RouteNavigateSession,
    private val polylineSession: MapRoutePolylineSession,
    private val flags: FeatureFlags,
    private val bridgeLoader: () -> List<BridgePoint>,
    private val activeVehicleHeightM: () -> Double?,
    private val clearanceBufferM: () -> Double,
    private val stopIdFactory: () -> String = { newStopId() },
) {
    var sheetValue: RouteSheetValue = RouteSheetValue.Peek
        private set
    var route: Route? = routeSession.activeRoute()
        private set
    var navigatePathPoints: List<LatLngPoint> =
        navigateSession.activePath()?.points.orEmpty()
        private set
    var polylineRevision: Int = 0
        private set
    var currentLocation: LatLngPoint? = null
        private set
    var headingUpEnabled: Boolean = MapHeadingUpCatalog.isEnabled(flags)
        private set

    fun setSheetValue(value: RouteSheetValue) {
        sheetValue = value
    }

    /** ATNA-75: data source already swapped; rebind active route + clear navigate. */
    fun onDemoRevision() {
        route = routeSession.activeRoute()
        if (DemoModeCatalog.isEnabled(flags)) {
            navigateSession.clear()
            polylineSession.clear()
            navigatePathPoints = emptyList()
            polylineRevision += 1
            if (route != null && route!!.stops.isNotEmpty()) {
                sheetValue = RouteSheetValue.Half
            }
        }
    }

    /**
     * @return true when a route was created; false when verified endpoints were required
     * and missing/invalid.
     */
    fun createRoute(input: CreateRouteInput): Boolean {
        val requireStartEnd = RouteStartEndCatalog.requiresEndpointsOnCreate(flags)
        val start = if (requireStartEnd) {
            when (val built = VerifiedRouteEndpointBuilder.build(input.start)) {
                is VerifiedRouteEndpointBuilder.Result.Ok -> built.endpoint
                is VerifiedRouteEndpointBuilder.Result.Err -> return false
            }
        } else {
            null
        }
        val end = if (requireStartEnd) {
            when (val built = VerifiedRouteEndpointBuilder.build(input.end)) {
                is VerifiedRouteEndpointBuilder.Result.Ok -> built.endpoint
                is VerifiedRouteEndpointBuilder.Result.Err -> return false
            }
        } else {
            null
        }
        route = routeSession.createRoute(
            name = input.name,
            dateChoice = input.dateChoice,
            customDate = input.customDate,
            start = start,
            end = end,
        )
        navigateSession.clear()
        polylineSession.clear()
        navigatePathPoints = emptyList()
        polylineRevision += 1
        sheetValue = RouteSheetValue.Half
        return true
    }

    /**
     * @return true when a stop was added; false when verified mode rejected free text.
     */
    fun addStop(
        addressLabel: String,
        kind: StopKind,
        suggestion: AddressSuggestion?,
    ): Boolean {
        val requireVerified = VerifiedAddressCatalog.requiresVerifiedSuggestion(flags)
        return when (
            val built = VerifiedRouteStopBuilder.build(
                id = stopIdFactory(),
                kind = kind,
                suggestion = suggestion,
                freeTextLabel = addressLabel,
                requireVerified = requireVerified,
            )
        ) {
            is VerifiedRouteStopBuilder.Result.Ok -> {
                routeSession.addStop(built.stop)
                route = routeSession.activeRoute()
                polylineSession.clear()
                polylineRevision += 1
                true
            }
            is VerifiedRouteStopBuilder.Result.Err -> false
        }
    }

    suspend fun navigate(
        vehicle: Vehicle?,
        selection: NavigationAppSelection,
    ): RouteNavigateSession.Result {
        val result = navigateSession.start(
            flags = flags,
            route = route,
            vehicle = vehicle,
            bufferM = clearanceBufferM(),
            selection = selection,
            optimisedPath = polylineSession.lastOptimisedPath(),
            currentLocation = currentLocation,
        )
        if (result is RouteNavigateSession.Result.Ready) {
            navigatePathPoints = result.path.points
            polylineRevision += 1
        }
        return result
    }

    /**
     * Updates in-memory heading-up and persists when [flags] is a
     * [com.truckie1.flags.LocalFeatureFlags] (production wiring).
     */
    fun setHeadingUp(locked: Boolean) {
        headingUpEnabled = locked
        val local = flags as? com.truckie1.flags.LocalFeatureFlags ?: return
        MapHeadingUpCatalog.setEnabled(local, enabled = locked)
    }

    fun onLocationFix(fix: LocationFix?) {
        currentLocation = fix?.let { LatLngPoint(it.latitude, it.longitude) }
    }

    fun visibleBridges(showBridgeMarkers: Boolean): List<BridgePoint> {
        if (!showBridgeMarkers) return emptyList()
        return BridgeVisibilityPipeline.filter(
            BridgeVisibilityPipeline.Input(
                bridges = bridgeLoader(),
                flags = flags,
                vehicleHeightM = activeVehicleHeightM(),
                bufferM = clearanceBufferM(),
            ),
        )
    }

    fun summaryStrip(): String =
        route?.let { RouteSummary.strip(it) } ?: RouteSummary.emptyStrip()

    fun uiState(displayPathPoints: List<LatLngPoint>): MapHomeUiState =
        MapHomeUiState(
            route = route,
            sheetValue = sheetValue,
            summaryStrip = summaryStrip(),
            navigatePathPoints = displayPathPoints,
        )

    fun requireStartEnd(): Boolean =
        RouteStartEndCatalog.requiresEndpointsOnCreate(flags)

    fun showNavigateAction(): Boolean =
        RouteNavigateCatalog.showsNavigateAction(flags)

    fun showOrientationLock(): Boolean =
        MapHeadingUpCatalog.shouldShowOnMap(flags)

    fun nextDestinationAddress(): String? {
        val navigating = navigatePathPoints.size >= 2
        return NextDestinationDisplayCatalog.displayAddress(
            flags = flags,
            route = route,
            navigating = navigating,
        )
    }

}
