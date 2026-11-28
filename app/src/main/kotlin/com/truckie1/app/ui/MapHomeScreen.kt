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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
 * Map-first home layout host: full-bleed MapLibre + draggable route sheet
 * (peek / half / full). Domain mutations live in [MapHomeController];
 * sheet widgets live in RouteSheetContent.kt (ATNA-90).
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
    /** Map-chrome orientation lock (Recenter-style), gated by follow flags. */
    showOrientationLock: Boolean = false,
    onOrientationLockChange: (Boolean) -> Unit = {},
    /** Fused fix for ActiveTourPath approach (ATNA-99). */
    onLocationFixChange: (LocationFix?) -> Unit = {},
    bridges: List<BridgePoint> = emptyList(),
    showBridgeMarkers: Boolean = false,
    showStopMarkers: Boolean = false,
    /** Prefer street names over POI clutter (`ff_map_street_labels`). */
    prioritiseStreetLabels: Boolean = false,
    /**
     * Upcoming stop / end address while Navigate is active
     * (`ff_next_destination_display`). Null hides the banner.
     */
    nextDestinationAddress: String? = null,
    modifier: Modifier = Modifier,
) {
    var followState by remember { mutableStateOf(MapLocationFollow.initial()) }
    var recenterToken by remember { mutableStateOf(0) }
    val dragState = remember {
        AnchoredDraggableState(initialValue = state.sheetValue)
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(state = dragState)
    val density = LocalDensity.current
    val sheetScroll = rememberScrollState()

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

        val rawOffset = dragState.offset
        val sheetTopOffsetPx = if (rawOffset.isNaN()) {
            MapHomeCatalog.sheetAnchorOffsets(containerHeightPx)
                .getValue(dragState.targetValue)
        } else {
            rawOffset
        }
        val visibleSheetHeightPx = MapHomeCatalog.visibleSheetHeightPx(
            containerHeightPx = containerHeightPx,
            sheetTopOffsetPx = sheetTopOffsetPx,
        )
        val visibleSheetHeightDp = with(density) { visibleSheetHeightPx.toDp() }

        MapLibreMap(
            stops = state.route?.stops.orEmpty(),
            showLocationPuck = showLocationPuck,
            locationFollowEnabled = locationFollowEnabled && showLocationPuck,
            headingUpEnabled = headingUpEnabled && locationFollowEnabled && showLocationPuck,
            followState = followState,
            onFollowStateChange = { followState = it },
            onLocationFixChange = onLocationFixChange,
            recenterToken = recenterToken,
            routePathPoints = state.navigatePathPoints,
            bridges = bridges,
            showBridgeMarkers = showBridgeMarkers,
            showStopMarkers = showStopMarkers,
            prioritiseStreetLabels = prioritiseStreetLabels,
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

        val nextAddress = nextDestinationAddress
        if (!nextAddress.isNullOrBlank()) {
            // Compact phone chip — Auto gets a clearer surface at ATNA-10.
            NextDestinationBanner(
                address = nextAddress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 64.dp, vertical = 8.dp),
            )
        }

        val sheetShape = RoundedCornerShape(
            topStart = RouteSheetPolishCatalog.topCornerRadiusDp.dp,
            topEnd = RouteSheetPolishCatalog.topCornerRadiusDp.dp,
        )
        val contentPad = RouteSheetPolishCatalog.contentHorizontalPaddingDp.dp
        // Visible-height sheet: top offset + height = container bottom. Scroll and
        // nav/IME insets stay inside the on-screen drawer (ATNA-67).
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(visibleSheetHeightDp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = sheetTopOffsetPx
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
                    .navigationBarsPadding()
                    .imePadding(),
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
                            .weight(1f, fill = true)
                            .fillMaxWidth()
                            .verticalScroll(sheetScroll)
                            .padding(horizontal = contentPad)
                            .padding(bottom = 24.dp),
                    )
                }
            }
        }

        if (showOrientationLock && locationFollowEnabled && showLocationPuck) {
            val lockBottom = if (followState.showsRecenter) 160.dp else 96.dp
            MapOrientationLockButton(
                locked = headingUpEnabled,
                onClick = { onOrientationLockChange(!headingUpEnabled) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = lockBottom),
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
private fun MapOrientationLockButton(
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (locked) {
        stringResource(R.string.map_orientation_lock_on)
    } else {
        stringResource(R.string.map_orientation_lock_off)
    }
    MapChromeIconButton(
        onClick = onClick,
        imageVector = if (locked) Icons.Filled.Navigation else Icons.Filled.Explore,
        contentDescription = description,
        testTag = MapHeadingUpCatalog.MAP_CONTROL_TEST_TAG,
        modifier = modifier,
    )
}

@Composable
private fun MapRecenterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.map_recenter)
    MapChromeIconButton(
        onClick = onClick,
        imageVector = Icons.Filled.MyLocation,
        contentDescription = description,
        testTag = MapChromeIconControls.RECENTER_TEST_TAG,
        modifier = modifier,
    )
}

/**
 * Shared primary circle FAB used by Recenter and orientation lock.
 * Core requirement: consistent visual treatment — only the glyph differs.
 */
@Composable
private fun MapChromeIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = MapChromeIconControls.shadowElevationDp.dp,
        modifier = modifier
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription },
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
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
