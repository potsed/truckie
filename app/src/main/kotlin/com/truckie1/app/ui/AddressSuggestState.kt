package com.truckie1.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.truckie1.app.routing.AsyncRoutingIoCatalog
import com.truckie1.app.trips.AddressAutocomplete
import com.truckie1.app.trips.AddressSuggestion
import kotlinx.coroutines.delay

/**
 * Async Nominatim / autocomplete suggestions for Compose (ATNA-88).
 *
 * Never calls [AddressAutocomplete.suggest] from `remember` — uses
 * [LaunchedEffect] + debounce so live HTTP stays off the composition thread.
 */
object AddressSuggestState {
    @Composable
    fun rememberSuggestions(
        query: String,
        autocomplete: AddressAutocomplete,
        debounceMs: Long = AsyncRoutingIoCatalog.SUGGEST_DEBOUNCE_MS,
    ): List<AddressSuggestion> {
        var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
        LaunchedEffect(query, autocomplete, debounceMs) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                suggestions = emptyList()
                return@LaunchedEffect
            }
            delay(debounceMs)
            suggestions = autocomplete.suggest(trimmed)
        }
        return suggestions
    }
}
