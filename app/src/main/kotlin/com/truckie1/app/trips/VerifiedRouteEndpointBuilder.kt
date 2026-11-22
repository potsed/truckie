package com.truckie1.app.trips

import com.truckie1.trips.RouteEndpoint

/**
 * Builds a verified [RouteEndpoint] from an autocomplete suggestion.
 */
object VerifiedRouteEndpointBuilder {
    const val ERR_VERIFIED_REQUIRED =
        "Choose a verified address from autocomplete suggestions"
    const val ERR_COORDINATES_REQUIRED =
        "Verified address must include latitude and longitude"

    sealed class Result {
        data class Ok(val endpoint: RouteEndpoint) : Result()
        data class Err(val message: String) : Result()
    }

    fun build(suggestion: AddressSuggestion?): Result {
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
            RouteEndpoint(
                addressLabel = suggestion.label.trim(),
                latitude = suggestion.latitude!!,
                longitude = suggestion.longitude!!,
            ),
        )
    }
}
