package com.truckie1.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.truckie1.app.R
import com.truckie1.app.trips.AddressAutocomplete
import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.app.trips.NoOpAddressAutocomplete
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteStop
import com.truckie1.trips.RouteSummary
import com.truckie1.trips.StopKind
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class MapHomeUiState(
    val route: Route?,
    val sheetValue: RouteSheetValue = RouteSheetValue.Peek,
    val summaryStrip: String = RouteSummary.emptyStrip(),
    val navigatePathPoints: List<LatLngPoint> = emptyList(),
)

/**
 * Map-first home: full-bleed MapLibre + draggable route sheet (peek / half / full).
 * Global TopAppBar / bottom nav are suppressed for this destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeScreen(
    state: MapHomeUiState,
    onSheetValueChange: (RouteSheetValue) -> Unit,
    onCreateRoute: (CreateRouteInput) -> Unit,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    onNavigateRoute: () -> Unit = {},
    onNavigatePrimary: (TruckiePrimaryDestination) -> Unit = {},
    canNavigatePrimary: (TruckiePrimaryDestination) -> Boolean = { true },
    autocomplete: AddressAutocomplete = NoOpAddressAutocomplete,
    requireVerifiedAddress: Boolean = false,
    requireStartEnd: Boolean = false,
    showNavigateAction: Boolean = false,
    showLocationPuck: Boolean = false,
    locationFollowEnabled: Boolean = false,
    /** Course-up while following (`ff_map_heading_up`). */
    headingUpEnabled: Boolean = false,
    bridges: List<BridgePoint> = emptyList(),
    showBridgeMarkers: Boolean = false,
    /**
     * TEMPORARY / DEV: Jump to sample bridges (Demo mode). Map chrome only.
     * Gated by [JumpToSampleBridges.shouldShow] / `ff_demo_mode`.
     */
    showJumpToSampleBridges: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var followState by remember { mutableStateOf(MapLocationFollow.initial()) }
    var recenterToken by remember { mutableStateOf(0) }
    var jumpToBridgesToken by remember { mutableStateOf(0) }
    val dragState = remember {
        AnchoredDraggableState(initialValue = state.sheetValue)
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(state = dragState)

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .distinctUntilChanged()
            .collect { onSheetValueChange(it) }
    }

    LaunchedEffect(state.sheetValue) {
        if (dragState.settledValue != state.sheetValue &&
            dragState.anchors.hasPositionFor(state.sheetValue)
        ) {
            dragState.animateTo(state.sheetValue)
        }
    }

    val nestedScroll = remember(dragState) {
        sheetNestedScrollConnection(dragState)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_home_root"),
    ) {
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val anchors = remember(containerHeightPx) {
            val offsets = MapHomeCatalog.sheetAnchorOffsets(containerHeightPx)
            DraggableAnchors {
                RouteSheetValue.Peek at offsets.getValue(RouteSheetValue.Peek)
                RouteSheetValue.Half at offsets.getValue(RouteSheetValue.Half)
                RouteSheetValue.Full at offsets.getValue(RouteSheetValue.Full)
            }
        }
        // Sync before draw so requireOffset() is safe on first layout.
        SideEffect {
            if (dragState.anchors != anchors) {
                dragState.updateAnchors(anchors, dragState.targetValue)
            }
        }

        MapLibreMap(
            stops = state.route?.stops.orEmpty(),
            showLocationPuck = showLocationPuck,
            locationFollowEnabled = locationFollowEnabled && showLocationPuck,
            headingUpEnabled = headingUpEnabled && locationFollowEnabled && showLocationPuck,
            followState = followState,
            onFollowStateChange = { followState = it },
            recenterToken = recenterToken,
            jumpToBridgesToken = jumpToBridgesToken,
            routePathPoints = state.navigatePathPoints,
            bridges = bridges,
            showBridgeMarkers = showBridgeMarkers,
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_libre_surface"),
        )

        MapHomeOverflowMenu(
            onNavigatePrimary = onNavigatePrimary,
            canNavigatePrimary = canNavigatePrimary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
        )

        val sheetShape = RoundedCornerShape(
            topStart = RouteSheetPolishCatalog.topCornerRadiusDp.dp,
            topEnd = RouteSheetPolishCatalog.topCornerRadiusDp.dp,
        )
        val contentPad = RouteSheetPolishCatalog.contentHorizontalPaddingDp.dp
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = 0,
                        y = dragState
                            .requireOffset()
                            .roundToInt()
                            .coerceIn(0, containerHeightPx.roundToInt()),
                    )
                }
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    flingBehavior = flingBehavior,
                )
                .nestedScroll(nestedScroll)
                .clip(sheetShape)
                .testTag("route_drag_sheet")
                .semantics {
                    contentDescription = "Route drag sheet ${dragState.currentValue}"
                },
            shape = sheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = RouteSheetPolishCatalog.tonalElevationDp.dp,
            shadowElevation = RouteSheetPolishCatalog.shadowElevationDp.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                // Drag handle only — no clickable here so vertical drag reaches
                // AnchoredDraggable (clickable was consuming the gesture).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomSheetDefaults.DragHandle(
                        modifier = Modifier.testTag("sheet_drag_handle"),
                        width = RouteSheetPolishCatalog.dragHandleWidthDp.dp,
                        height = RouteSheetPolishCatalog.dragHandleHeightDp.dp,
                        color = if (RouteSheetPolishCatalog.dragHandleUsesOnSurfaceVariant) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                    )
                }

                if (MapHomeCatalog.showsSummaryStrip(dragState.currentValue)) {
                    Text(
                        text = state.summaryStrip,
                        style = if (RouteSheetPolishCatalog.summaryUsesTitleSmall) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentPad)
                            .padding(
                                top = RouteSheetPolishCatalog.peekContentVerticalPaddingDp.dp,
                                bottom = 8.dp,
                            )
                            .testTag("route_summary_strip"),
                    )
                }

                if (MapHomeCatalog.showsCreateRouteForm(dragState.currentValue) ||
                    MapHomeCatalog.showsStopList(dragState.currentValue)
                ) {
                    RouteSheetBody(
                        route = state.route,
                        sheetValue = dragState.currentValue,
                        onCreateRoute = onCreateRoute,
                        onAddStop = onAddStop,
                        onNavigateRoute = onNavigateRoute,
                        autocomplete = autocomplete,
                        requireVerifiedAddress = requireVerifiedAddress,
                        requireStartEnd = requireStartEnd,
                        showNavigateAction = showNavigateAction,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = contentPad)
                            .padding(bottom = 24.dp),
                    )
                }
            }
        }

        // TEMPORARY / DEV (ff_demo_mode): fly to SEQ sample bridges near Brisbane CBD.
        if (showJumpToSampleBridges) {
            MapJumpToSampleBridgesButton(
                onClick = {
                    val jump = JumpToSampleBridges.onJump(followState)
                    followState = jump.followState
                    jumpToBridgesToken += 1
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(
                        end = 16.dp,
                        bottom = if (
                            locationFollowEnabled &&
                            showLocationPuck &&
                            followState.showsRecenter
                        ) {
                            160.dp
                        } else {
                            96.dp
                        },
                    ),
            )
        }

        if (locationFollowEnabled && showLocationPuck && followState.showsRecenter) {
            MapRecenterButton(
                onClick = {
                    followState = MapLocationFollow.onRecenter(followState)
                    recenterToken += 1
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 96.dp),
            )
        }
    }
}

@Composable
private fun MapJumpToSampleBridgesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 4.dp,
        modifier = modifier
            .testTag(JumpToSampleBridges.TEST_TAG)
            .semantics { contentDescription = JumpToSampleBridges.SHORT_LABEL }
            .clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(R.string.map_jump_to_sample_bridges),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MapRecenterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
        modifier = modifier
            .testTag("map_recenter_button")
            .semantics { contentDescription = "Recenter on my location" },
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(R.string.map_recenter),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Nested-scroll bridge so dragging the sheet content (when scrolled to edges)
 * continues to move the sheet — same contract as Material3 bottom sheets.
 */
private fun sheetNestedScrollConnection(
    state: AnchoredDraggableState<RouteSheetValue>,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            Offset(0f, state.dispatchRawDelta(delta))
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val delta = available.y
        return if (source == NestedScrollSource.UserInput) {
            Offset(0f, state.dispatchRawDelta(delta))
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.y
        return if (toFling < 0 && state.offset > state.anchors.minPosition()) {
            val target = state.anchors.closestAnchor(
                state.requireOffset(),
                searchUpwards = true,
            ) ?: state.currentValue
            state.animateTo(target)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val target = state.anchors.closestAnchor(state.requireOffset())
            ?: state.currentValue
        state.animateTo(target)
        return available
    }
}

@Composable
private fun MapHomeOverflowMenu(
    onNavigatePrimary: (TruckiePrimaryDestination) -> Unit,
    canNavigatePrimary: (TruckiePrimaryDestination) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp,
            modifier = Modifier.testTag("map_home_menu"),
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.map_home_menu),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val items = listOf(
                Triple(
                    TruckiePrimaryDestination.PLAN_TRIP,
                    Icons.Filled.Place,
                    R.string.nav_plan_trip,
                ),
                Triple(
                    TruckiePrimaryDestination.VEHICLES,
                    Icons.Filled.DirectionsCar,
                    R.string.nav_vehicles,
                ),
                Triple(
                    TruckiePrimaryDestination.SETTINGS,
                    Icons.Filled.Settings,
                    R.string.nav_settings,
                ),
            )
            items.forEach { (destination, icon, labelRes) ->
                val enabled = canNavigatePrimary(destination)
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    leadingIcon = {
                        Icon(imageVector = icon, contentDescription = null)
                    },
                    enabled = enabled,
                    onClick = {
                        expanded = false
                        onNavigatePrimary(destination)
                    },
                )
            }
        }
    }
}

@Composable
internal fun RouteSheetBody(
    route: Route?,
    sheetValue: RouteSheetValue,
    onCreateRoute: (CreateRouteInput) -> Unit,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    onNavigateRoute: () -> Unit = {},
    autocomplete: AddressAutocomplete = NoOpAddressAutocomplete,
    requireVerifiedAddress: Boolean = false,
    requireStartEnd: Boolean = false,
    showNavigateAction: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("route_sheet_body")) {
        if (route == null && MapHomeCatalog.showsCreateRouteForm(sheetValue)) {
            CreateRouteForm(
                onCreateRoute = onCreateRoute,
                autocomplete = autocomplete,
                requireStartEnd = requireStartEnd,
            )
        } else if (route != null) {
            if (showNavigateAction && MapHomeCatalog.showsCreateRouteForm(sheetValue)) {
                NavigateRouteButton(
                    enabled = route.hasEndpoints,
                    onClick = onNavigateRoute,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (MapHomeCatalog.showsCreateRouteForm(sheetValue)) {
                if (route.hasEndpoints) {
                    RouteEndpointsSummary(route = route)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                AddFindStopsSection(
                    autocomplete = autocomplete,
                    onAddStop = onAddStop,
                    requireVerifiedAddress = requireVerifiedAddress,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (MapHomeCatalog.showsStopList(sheetValue)) {
                StopListSection(stops = route.stops)
            }
        }
    }
}

@Composable
private fun NavigateRouteButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TruckiePrimaryButton(
        text = stringResource(R.string.navigate_route),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("navigate_route_button"),
    )
}

@Composable
private fun RouteEndpointsSummary(route: Route) {
    val start = route.start ?: return
    val end = route.end ?: return
    Text(
        text = if (route.isRoundTrip) {
            stringResource(R.string.route_round_trip)
        } else {
            "${start.addressLabel} → ${end.addressLabel}"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("route_endpoints_summary"),
    )
}

@Composable
private fun CreateRouteForm(
    onCreateRoute: (CreateRouteInput) -> Unit,
    autocomplete: AddressAutocomplete = NoOpAddressAutocomplete,
    requireStartEnd: Boolean = false,
) {
    var name by remember { mutableStateOf("") }
    var dateChoice by remember { mutableStateOf(RouteDateChoice.TODAY) }
    var startSuggestion by remember { mutableStateOf<AddressSuggestion?>(null) }
    var endSuggestion by remember { mutableStateOf<AddressSuggestion?>(null) }
    var roundTrip by remember { mutableStateOf(false) }
    var startQuery by remember { mutableStateOf("") }
    var endQuery by remember { mutableStateOf("") }
    val today = remember { LocalDate.now() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    }
    val sectionGap = RouteSheetPolishCatalog.sectionSpacingDp.dp
    val startSuggestions = remember(startQuery) { autocomplete.suggest(startQuery) }
    val endSuggestions = remember(endQuery) { autocomplete.suggest(endQuery) }
    val endpointsReady = !requireStartEnd || (
        startSuggestion?.isVerified == true &&
            (roundTrip || endSuggestion?.isVerified == true)
        )

    Text(
        text = stringResource(R.string.create_route_title),
        style = if (RouteSheetPolishCatalog.createRouteUsesHeadlineTitle) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.titleLarge
        },
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("create_route_title"),
    )
    Spacer(modifier = Modifier.height(sectionGap))
    Text(
        text = stringResource(R.string.route_name_optional),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_name_field"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(R.string.route_name_hint)) },
        colors = highContrastFieldColors(),
    )
    if (requireStartEnd) {
        Spacer(modifier = Modifier.height(sectionGap))
        EndpointAddressField(
            label = stringResource(R.string.route_start_label),
            query = startQuery,
            onQueryChange = {
                startQuery = it
                startSuggestion = null
            },
            suggestions = startSuggestions,
            selected = startSuggestion,
            onSelect = {
                startSuggestion = it
                startQuery = it.label
                if (roundTrip) {
                    endSuggestion = it
                    endQuery = it.label
                }
            },
            placeholder = stringResource(R.string.route_start_hint),
            fieldTestTag = "route_start_field",
            suggestionTestTag = "route_start_suggestion",
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("route_round_trip_row")
                .clickable {
                    roundTrip = !roundTrip
                    if (roundTrip && startSuggestion != null) {
                        endSuggestion = startSuggestion
                        endQuery = startSuggestion!!.label
                    }
                },
        ) {
            Checkbox(
                checked = roundTrip,
                onCheckedChange = { checked ->
                    roundTrip = checked
                    if (checked && startSuggestion != null) {
                        endSuggestion = startSuggestion
                        endQuery = startSuggestion!!.label
                    }
                },
                modifier = Modifier.testTag("route_round_trip_checkbox"),
            )
            Text(
                text = stringResource(R.string.route_round_trip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (!roundTrip) {
            Spacer(modifier = Modifier.height(12.dp))
            EndpointAddressField(
                label = stringResource(R.string.route_end_label),
                query = endQuery,
                onQueryChange = {
                    endQuery = it
                    endSuggestion = null
                },
                suggestions = endSuggestions,
                selected = endSuggestion,
                onSelect = {
                    endSuggestion = it
                    endQuery = it.label
                },
                placeholder = stringResource(R.string.route_end_hint),
                fieldTestTag = "route_end_field",
                suggestionTestTag = "route_end_suggestion",
            )
        }
    }
    Spacer(modifier = Modifier.height(sectionGap))
    Text(
        text = stringResource(R.string.select_date),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    DateChoiceRow(
        label = stringResource(R.string.date_today, today.format(formatter)),
        selected = dateChoice == RouteDateChoice.TODAY,
        onSelect = { dateChoice = RouteDateChoice.TODAY },
        testTag = "date_today",
        cardTestTag = "date_today_card",
    )
    DateChoiceRow(
        label = stringResource(R.string.date_tomorrow, today.plusDays(1).format(formatter)),
        selected = dateChoice == RouteDateChoice.TOMORROW,
        onSelect = { dateChoice = RouteDateChoice.TOMORROW },
        testTag = "date_tomorrow",
        cardTestTag = "date_tomorrow_card",
    )
    DateChoiceRow(
        label = stringResource(R.string.date_pick),
        selected = dateChoice == RouteDateChoice.CUSTOM,
        onSelect = { dateChoice = RouteDateChoice.CUSTOM },
        testTag = "date_custom",
        cardTestTag = "date_custom_card",
    )
    Spacer(modifier = Modifier.height(sectionGap))
    TruckiePrimaryButton(
        text = stringResource(R.string.confirm_route),
        onClick = {
            if (!endpointsReady) return@TruckiePrimaryButton
            val custom = if (dateChoice == RouteDateChoice.CUSTOM) {
                today.plusDays(7)
            } else {
                null
            }
            val end = if (roundTrip) startSuggestion else endSuggestion
            onCreateRoute(
                CreateRouteInput(
                    name = name.ifBlank { null },
                    dateChoice = dateChoice,
                    customDate = custom,
                    start = startSuggestion,
                    end = end,
                    roundTrip = roundTrip,
                ),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_route_button"),
        enabled = endpointsReady,
    )
}

@Composable
private fun EndpointAddressField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<AddressSuggestion>,
    selected: AddressSuggestion?,
    onSelect: (AddressSuggestion) -> Unit,
    placeholder: String,
    fieldTestTag: String,
    suggestionTestTag: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(fieldTestTag),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = highContrastFieldColors(),
    )
    if (selected?.isVerified == true) {
        Text(
            text = selected.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("${fieldTestTag}_selected"),
        )
    }
    if (suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag(suggestionTestTag)
                    .clickable { onSelect(suggestion) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = suggestion.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DateChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
    cardTestTag: String,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    }
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .border(
                width = if (RouteSheetPolishCatalog.dateChoicesUseOutlinedCards) 1.5.dp else 0.dp,
                color = if (RouteSheetPolishCatalog.dateChoicesUseOutlinedCards) {
                    borderColor
                } else {
                    Color.Transparent
                },
                shape = shape,
            )
            .background(background)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            // Prefer the polished card tag; legacy tag kept as alias for older assertions.
            .testTag(cardTestTag)
            .semantics { contentDescription = testTag },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun AddFindStopsSection(
    autocomplete: AddressAutocomplete,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    requireVerifiedAddress: Boolean = false,
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(StopKind.PICKUP_LOAD) }
    var selectedSuggestion by remember { mutableStateOf<AddressSuggestion?>(null) }
    val suggestions = remember(query) { autocomplete.suggest(query) }
    val sectionGap = RouteSheetPolishCatalog.sectionSpacingDp.dp

    Text(
        text = stringResource(R.string.add_or_find_stops),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("add_find_stops_title"),
    )
    Spacer(modifier = Modifier.height(12.dp))
    // Stable tag for polish tests (TextField decoration icons are unmerged / fragile).
    if (RouteSheetPolishCatalog.addFindUsesSearchLeadingIcon) {
        Spacer(modifier = Modifier.size(1.dp).testTag("add_find_search_icon"))
    }
    OutlinedTextField(
        value = query,
        onValueChange = {
            query = it
            selectedSuggestion = null
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_find_stops_field"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(R.string.hint_address)) },
        leadingIcon = if (RouteSheetPolishCatalog.addFindUsesSearchLeadingIcon) {
            {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.add_or_find_stops),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        colors = highContrastFieldColors(),
    )
    if (suggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        suggestions.forEach { suggestion ->
            val suggestionShape = RoundedCornerShape(10.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("address_suggestion")
                    .clickable {
                        if (requireVerifiedAddress) {
                            if (suggestion.isVerified) {
                                onAddStop(suggestion.label, kind, suggestion)
                                query = ""
                                selectedSuggestion = null
                            }
                        } else {
                            query = suggestion.label
                            selectedSuggestion = suggestion
                            onAddStop(suggestion.label, kind, suggestion)
                            query = ""
                            selectedSuggestion = null
                        }
                    },
                shape = suggestionShape,
                color = if (RouteSheetPolishCatalog.suggestionsUseSurfaceRows) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = 1.dp,
            ) {
                Text(
                    text = suggestion.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
    if (requireVerifiedAddress && query.isNotBlank() && suggestions.isEmpty()) {
        Text(
            text = stringResource(R.string.verified_address_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("verified_address_hint"),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("stop_kind_chip_row"),
    ) {
        StopKindChip(
            label = stringResource(R.string.stop_kind_pickup_load),
            selected = kind == StopKind.PICKUP_LOAD,
            onClick = { kind = StopKind.PICKUP_LOAD },
            testTag = "kind_pickup_load",
        )
        StopKindChip(
            label = stringResource(R.string.stop_kind_drop_off_unload),
            selected = kind == StopKind.DROP_OFF_UNLOAD,
            onClick = { kind = StopKind.DROP_OFF_UNLOAD },
            testTag = "kind_drop_off_unload",
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    val canAddFreeText = !requireVerifiedAddress && query.isNotBlank()
    TruckiePrimaryButton(
        text = stringResource(R.string.add_stop_to_route),
        onClick = {
            if (requireVerifiedAddress) return@TruckiePrimaryButton
            val label = query.trim()
            if (label.isNotEmpty()) {
                onAddStop(label, kind, selectedSuggestion)
                query = ""
                selectedSuggestion = null
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_stop_button"),
        enabled = canAddFreeText,
    )
    Spacer(modifier = Modifier.height(sectionGap))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopKindChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    if (RouteSheetPolishCatalog.stopKindsUseFilterChips) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            modifier = Modifier.testTag(testTag),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                selectedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.testTag(testTag),
        ) {
            Text(
                text = label,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StopListSection(stops: List<RouteStop>) {
    Text(
        text = stringResource(R.string.stop_list_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("stop_list_title"),
    )
    Spacer(modifier = Modifier.height(12.dp))
    if (stops.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stop_list_empty"),
        ) {
            Text(
                text = stringResource(R.string.no_route_stops_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            )
        }
    } else {
        stops.forEachIndexed { index, stop ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("stop_row_${stop.id}"),
                verticalAlignment = Alignment.Top,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("stop_timeline_badge_${stop.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (stop.order + 1).toString().padStart(2, '0'),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (RouteSheetPolishCatalog.stopRowsUseTimelineBadge &&
                        index < stops.lastIndex
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(2.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.addressLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("stop_kind_label_${stop.id}"),
                    ) {
                        Text(
                            text = when (stop.kind) {
                                StopKind.PICKUP_LOAD ->
                                    stringResource(R.string.stop_kind_pickup_load)
                                StopKind.DROP_OFF_UNLOAD ->
                                    stringResource(R.string.stop_kind_drop_off_unload)
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (index < stops.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}

@Composable
private fun highContrastFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

internal fun nearestSheetValue(fraction: Float): RouteSheetValue {
    val peek = MapHomeCatalog.peekFraction
    val half = MapHomeCatalog.halfFraction
    val full = MapHomeCatalog.fullFraction
    val dPeek = kotlin.math.abs(fraction - peek)
    val dHalf = kotlin.math.abs(fraction - half)
    val dFull = kotlin.math.abs(fraction - full)
    return when {
        dPeek <= dHalf && dPeek <= dFull -> RouteSheetValue.Peek
        dHalf <= dFull -> RouteSheetValue.Half
        else -> RouteSheetValue.Full
    }
}

/** Test helper — builds a stop id without leaking UUID into callers. */
fun newStopId(): String = UUID.randomUUID().toString()
