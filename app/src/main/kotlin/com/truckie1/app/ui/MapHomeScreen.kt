package com.truckie1.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.truckie1.app.R
import com.truckie1.app.trips.AddressAutocomplete
import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.app.trips.NoOpAddressAutocomplete
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteStop
import com.truckie1.trips.RouteSummary
import com.truckie1.trips.StopKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class MapHomeUiState(
    val route: Route?,
    val sheetValue: RouteSheetValue = RouteSheetValue.Peek,
    val summaryStrip: String = RouteSummary.emptyStrip(),
)

@Composable
fun MapHomeScreen(
    state: MapHomeUiState,
    onSheetValueChange: (RouteSheetValue) -> Unit,
    onCreateRoute: (name: String?, dateChoice: RouteDateChoice, customDate: LocalDate?) -> Unit,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    autocomplete: AddressAutocomplete = NoOpAddressAutocomplete,
    modifier: Modifier = Modifier,
) {
    var sheetFraction by remember(state.sheetValue) {
        mutableFloatStateOf(MapHomeCatalog.fractionFor(state.sheetValue))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_home_root"),
    ) {
        MapLibreMap(
            stops = state.route?.stops.orEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_libre_surface"),
        )

        RouteDragSheet(
            fraction = sheetFraction,
            sheetValue = state.sheetValue,
            summaryStrip = state.summaryStrip,
            route = state.route,
            onFractionChange = { fraction ->
                sheetFraction = fraction.coerceIn(
                    MapHomeCatalog.peekFraction,
                    MapHomeCatalog.fullFraction,
                )
            },
            onSheetSettled = { value ->
                sheetFraction = MapHomeCatalog.fractionFor(value)
                onSheetValueChange(value)
            },
            onCreateRoute = onCreateRoute,
            onAddStop = onAddStop,
            autocomplete = autocomplete,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RouteDragSheet(
    fraction: Float,
    sheetValue: RouteSheetValue,
    summaryStrip: String,
    route: Route?,
    onFractionChange: (Float) -> Unit,
    onSheetSettled: (RouteSheetValue) -> Unit,
    onCreateRoute: (name: String?, dateChoice: RouteDateChoice, customDate: LocalDate?) -> Unit,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    autocomplete: AddressAutocomplete,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("route_drag_sheet")
            .semantics { contentDescription = "Route drag sheet $sheetValue" },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                val delta = -dragAmount / size.height.toFloat()
                                onFractionChange(fraction + delta)
                            },
                            onDragEnd = {
                                onSheetSettled(nearestSheetValue(fraction))
                            },
                        )
                    }
                    .clickable {
                        val next = when (sheetValue) {
                            RouteSheetValue.Peek -> RouteSheetValue.Half
                            RouteSheetValue.Half -> RouteSheetValue.Full
                            RouteSheetValue.Full -> RouteSheetValue.Peek
                        }
                        onSheetSettled(next)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                        .testTag("sheet_drag_handle"),
                )
            }

            if (MapHomeCatalog.showsSummaryStrip(sheetValue)) {
                Text(
                    text = summaryStrip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("route_summary_strip"),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (MapHomeCatalog.showsCreateRouteForm(sheetValue) ||
                MapHomeCatalog.showsStopList(sheetValue)
            ) {
                RouteSheetBody(
                    route = route,
                    sheetValue = sheetValue,
                    onCreateRoute = onCreateRoute,
                    onAddStop = onAddStop,
                    autocomplete = autocomplete,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
internal fun RouteSheetBody(
    route: Route?,
    sheetValue: RouteSheetValue,
    onCreateRoute: (name: String?, dateChoice: RouteDateChoice, customDate: LocalDate?) -> Unit,
    onAddStop: (addressLabel: String, kind: StopKind, suggestion: AddressSuggestion?) -> Unit,
    autocomplete: AddressAutocomplete = NoOpAddressAutocomplete,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("route_sheet_body")) {
        if (route == null && MapHomeCatalog.showsCreateRouteForm(sheetValue)) {
            CreateRouteForm(onCreateRoute = onCreateRoute)
        } else if (route != null) {
            if (MapHomeCatalog.showsCreateRouteForm(sheetValue)) {
                AddFindStopsSection(
                    autocomplete = autocomplete,
                    onAddStop = onAddStop,
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
private fun CreateRouteForm(
    onCreateRoute: (name: String?, dateChoice: RouteDateChoice, customDate: LocalDate?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dateChoice by remember { mutableStateOf(RouteDateChoice.TODAY) }
    val today = remember { LocalDate.now() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    }

    Text(
        text = stringResource(R.string.create_route_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("create_route_title"),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.route_name_optional),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_name_field"),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.route_name_hint)) },
        colors = highContrastFieldColors(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.select_date),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DateChoiceRow(
        label = stringResource(R.string.date_today, today.format(formatter)),
        selected = dateChoice == RouteDateChoice.TODAY,
        onSelect = { dateChoice = RouteDateChoice.TODAY },
        testTag = "date_today",
    )
    DateChoiceRow(
        label = stringResource(R.string.date_tomorrow, today.plusDays(1).format(formatter)),
        selected = dateChoice == RouteDateChoice.TOMORROW,
        onSelect = { dateChoice = RouteDateChoice.TOMORROW },
        testTag = "date_tomorrow",
    )
    DateChoiceRow(
        label = stringResource(R.string.date_pick),
        selected = dateChoice == RouteDateChoice.CUSTOM,
        onSelect = { dateChoice = RouteDateChoice.CUSTOM },
        testTag = "date_custom",
    )
    Spacer(modifier = Modifier.height(16.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.confirm_route),
        onClick = {
            val custom = if (dateChoice == RouteDateChoice.CUSTOM) {
                today.plusDays(7)
            } else {
                null
            }
            onCreateRoute(name.ifBlank { null }, dateChoice, custom)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_route_button"),
    )
}

@Composable
private fun DateChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
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
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(StopKind.PICKUP_LOAD) }
    val suggestions = remember(query) { autocomplete.suggest(query) }

    Text(
        text = stringResource(R.string.add_or_find_stops),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("add_find_stops_title"),
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_find_stops_field"),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.hint_address)) },
        colors = highContrastFieldColors(),
    )
    if (suggestions.isNotEmpty()) {
        suggestions.forEach { suggestion ->
            Text(
                text = suggestion.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        query = suggestion.label
                        onAddStop(suggestion.label, kind, suggestion)
                        query = ""
                    }
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Spacer(modifier = Modifier.height(8.dp))
    TruckiePrimaryButton(
        text = stringResource(R.string.add_stop_to_route),
        onClick = {
            val label = query.trim()
            if (label.isNotEmpty()) {
                onAddStop(label, kind, null)
                query = ""
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_stop_button"),
        enabled = query.isNotBlank(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopKindChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
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

@Composable
private fun StopListSection(stops: List<RouteStop>) {
    Text(
        text = stringResource(R.string.stop_list_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("stop_list_title"),
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (stops.isEmpty()) {
        Text(
            text = stringResource(R.string.no_route_stops_yet),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("stop_list_empty"),
        )
    } else {
        stops.forEach { stop ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("stop_row_${stop.id}"),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (stop.order + 1).toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.addressLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = when (stop.kind) {
                            StopKind.PICKUP_LOAD -> stringResource(R.string.stop_kind_pickup_load)
                            StopKind.DROP_OFF_UNLOAD -> stringResource(R.string.stop_kind_drop_off_unload)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            HorizontalDivider()
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
