package com.truckie1.app.ui

import com.truckie1.app.trips.AddressSuggestion
import com.truckie1.trips.RouteDateChoice
import java.time.LocalDate

/** Payload from the map-home Create route form. */
data class CreateRouteInput(
    val name: String?,
    val dateChoice: RouteDateChoice,
    val customDate: LocalDate?,
    val start: AddressSuggestion? = null,
    val end: AddressSuggestion? = null,
    val roundTrip: Boolean = false,
)
