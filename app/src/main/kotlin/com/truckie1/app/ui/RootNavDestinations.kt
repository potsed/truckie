package com.truckie1.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.truckie1.app.R
import com.truckie1.app.TruckieApp
import com.truckie1.app.AndroidAutoGate
import com.truckie1.app.profiles.HomeVehicleSummary
import com.truckie1.app.profiles.VehicleFormParser
import com.truckie1.app.routing.ActiveTourPath
import com.truckie1.app.routing.GeographicTourOrderCatalog
import com.truckie1.app.trips.HomeTripSummary
import com.truckie1.app.trips.OptimizeForVehicleGate
import com.truckie1.app.trips.OptimizeResultSummary
import com.truckie1.app.trips.StopFormParser
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.OptimizeResult
import com.truckie1.trips.CargoAction
import com.truckie1.trips.ClearanceCalculator
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TourOptimizeGoals
import com.truckie1.trips.TripStop
import kotlinx.coroutines.launch

/**
 * Root NavHost destination controllers (ATNA-90 / P1-5).
 * Extracted from TruckieRootNav so the chrome shell stays thin.
 * Legacy Activities are **not** deleted here (ATNA-74).
 *
 * Gherkin: features/map_home_orchestration.feature
 */

internal fun bindHomeState(app: TruckieApp): HomeUiState {
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
internal fun ColumnScope.TripStopsRoute(
    app: TruckieApp,
    demoRevision: Int = 0,
    onAddStop: () -> Unit,
    onBlocked: (String) -> Unit,
) {
    val context = LocalContext.current
    val optimizeScope = rememberCoroutineScope()
    var stops by remember { mutableStateOf(app.tripSession.stops()) }
    var clearanceSummary by remember { mutableStateOf("") }
    var showOptimize by remember {
        mutableStateOf(OptimizeForVehicleGate.isAvailable(app.featureFlags))
    }
    var optimizeGoals by remember { mutableStateOf(TourOptimizeGoals.DEFAULT) }
    var optimizeStatus by remember { mutableStateOf<String?>(null) }

    fun bindList() {
        val current = app.tripSession.stops()
        stops = current
        clearanceSummary = tripClearanceText(app, current)
        showOptimize = OptimizeForVehicleGate.isAvailable(app.featureFlags)
    }

    LaunchedEffect(demoRevision) { bindList() }

    TripStopsScreen(
        stops = stops,
        clearanceSummary = clearanceSummary,
        showOptimize = showOptimize,
        optimizeGoals = optimizeGoals,
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
        onGoalsChange = { optimizeGoals = it },
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
            val geographic = GeographicTourOrderCatalog.isEnabled(app.featureFlags)
            val route = app.routeSession.activeRoute()
            val optimizeStops = if (geographic && route != null && route.hasEndpoints) {
                ActiveTourPath.tourStopsFrom(route)
            } else {
                currentStops
            }
            optimizeScope.launch {
                when (
                    val result = app.routeOptimizer.optimize(
                        stops = optimizeStops,
                        vehicle = vehicle,
                        bufferM = app.clearanceBufferM,
                        goals = optimizeGoals,
                    )
                ) {
                    is OptimizeResult.Ok -> {
                        val persisted = if (geographic) {
                            GeographicTourOrderCatalog.intermediateStops(result.orderedStops)
                        } else {
                            result.orderedStops
                        }
                        app.tripSession.replaceStops(persisted)
                        app.tripSession.lastOptimizeMode =
                            optimizeGoals.asLegacyMode() ?: OptimizeMode.FASTEST
                        app.mapRoutePolylineSession.rememberOptimised(result.path)
                        optimizeStatus = context.getString(
                            R.string.optimize_success,
                            result.mode.name.lowercase().replaceFirstChar { it.titlecase() },
                            result.route.durationSeconds,
                            OptimizeResultSummary.pathSourceLabel(result),
                        )
                        bindList()
                    }
                    is OptimizeResult.Blocked -> onBlocked(result.reason)
                }
            }
        },
    )
}

internal fun tripClearanceText(app: TruckieApp, stops: List<TripStop>): String {
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
internal fun ColumnScope.VehicleListRoute(
    app: TruckieApp,
    demoRevision: Int = 0,
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

    LaunchedEffect(demoRevision) { bindList() }

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
internal fun AddStopRoute(
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

internal fun stageCurrentCargoRow(form: AddStopFormState): StopFormParser.CargoInput? {
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
internal fun VehicleEditRoute(
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

internal fun Vehicle.toEditFormState(regoEnabled: Boolean): VehicleEditFormState = VehicleEditFormState(
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
