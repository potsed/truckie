package com.truckie1.app.trips

import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind

/**
 * Builds a [RouteStop] from a verified autocomplete suggestion (or free text when allowed).
 */
object VerifiedRouteStopBuilder {
    const val ERR_VERIFIED_REQUIRED =
        "Choose a verified address from autocomplete suggestions"
    const val ERR_ADDRESS_REQUIRED = "Address is required"
    const val ERR_COORDINATES_REQUIRED =
        "Verified address must include latitude and longitude"

    sealed class Result {
        data class Ok(val stop: RouteStop) : Result()
        data class Err(val message: String) : Result()
    }

    fun build(
        id: String,
        kind: StopKind,
        suggestion: AddressSuggestion?,
        freeTextLabel: String?,
        requireVerified: Boolean,
        order: Int = 0,
    ): Result {
        if (requireVerified) {
            if (suggestion == null || !suggestion.isVerified) {
                return if (suggestion != null &&
                    (suggestion.latitude == null || suggestion.longitude == null)
                ) {
                    Result.Err(ERR_COORDINATES_REQUIRED)
                } else {
                    Result.Err(ERR_VERIFIED_REQUIRED)
                }
            }
            return Result.Ok(
                RouteStop(
                    id = id,
                    addressLabel = suggestion.label.trim(),
                    latitude = suggestion.latitude,
                    longitude = suggestion.longitude,
                    kind = kind,
                    order = order,
                ),
            )
        }

        if (suggestion != null && suggestion.isVerified) {
            return Result.Ok(
                RouteStop(
                    id = id,
                    addressLabel = suggestion.label.trim(),
                    latitude = suggestion.latitude,
                    longitude = suggestion.longitude,
                    kind = kind,
                    order = order,
                ),
            )
        }

        val label = freeTextLabel?.trim().orEmpty()
        if (label.isEmpty()) return Result.Err(ERR_ADDRESS_REQUIRED)
        return Result.Ok(
            RouteStop(
                id = id,
                addressLabel = label,
                latitude = suggestion?.latitude,
                longitude = suggestion?.longitude,
                kind = kind,
                order = order,
            ),
        )
    }
}
