package com.truckie1.routing

import com.truckie1.profiles.Vehicle

/**
 * Stub GraphHopper Custom Model built from active vehicle + [TruckRoutingPreferences].
 *
 * Encodes bridges (max_height), weight (max_weight), tolls, and a **soft**
 * non-residential through-route preference in the request snippet shape.
 * Residential is never hard-blocked — deliveries to residential addresses remain OK.
 * Live evaluation + restriction packs remain Phase D.
 */
data class GraphHopperCustomModelStub(
    val requiredClearanceHeightM: Double,
    val requiredMassT: Double,
    val vehicleWidthM: Double,
    val vehicleLengthM: Double,
    val preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT,
) {
    val tollPreference: TollPreference
        get() = preferences.tollPreference

    val preferNonResidentialThroughRoutes: Boolean
        get() = preferences.preferNonResidentialThroughRoutes

    @Deprecated(
        "Use preferNonResidentialThroughRoutes (soft preference, not hard block)",
        ReplaceWith("preferNonResidentialThroughRoutes"),
    )
    val avoidResidentialWherePossible: Boolean
        get() = preferNonResidentialThroughRoutes

    fun toRequestSnippet(): String {
        val priority = buildList {
            if (preferences.avoidLowBridges) {
                add("""{ "if": "max_height < $requiredClearanceHeightM", "multiply_by": "0" }""")
            }
            if (preferences.avoidWeightRestrictedRoads) {
                add("""{ "if": "max_weight < $requiredMassT", "multiply_by": "0" }""")
            }
            if (preferences.preferNonResidentialThroughRoutes) {
                // Soft preference only (multiply_by > 0): prefer arterial/through roads.
                // Never hard-block — residential delivery start/end/stops must stay reachable.
                // When residential is required, minimise penetration (last-mile access only,
                // not as a through-route between non-residential segments).
                val residentialFactor = if (preferences.minimiseResidentialPenetration) {
                    "0.05"
                } else {
                    "0.1"
                }
                add("""{ "if": "road_class == RESIDENTIAL", "multiply_by": "$residentialFactor" }""")
            }
        }.joinToString(",\n                       ")
        // Among height-/weight-/toll-safe edges (with soft residential preference),
        // ask GraphHopper for the most efficient (FASTEST) or cost-effective (CHEAPEST) tour.
        val optimize = preferences.optimizeMode.name
        return """
        {
          "priority": [$priority],
          "areas": {},
          "toll": "${preferences.tollPreference.name}",
          "prefer_non_residential_through_routes": ${preferences.preferNonResidentialThroughRoutes},
          "minimise_residential_penetration": ${preferences.minimiseResidentialPenetration},
          "residential_soft_preference_only": true,
          "residential_delivery_access_ok": true,
          "avoid_low_bridges": ${preferences.avoidLowBridges},
          "avoid_weight_restricted": ${preferences.avoidWeightRestrictedRoads},
          "block_under_low_clearance": ${preferences.avoidLowBridges},
          "over_deck_ok": true,
          "bridge_passage_rule": "under_hard_block_over_ok",
          "optimize": "$optimize"
        }
        """.trimIndent()
    }

    companion object {
        /** Shared Navigate/Optimise fallback when no vehicle-derived model is supplied. */
        const val DEFAULT_CLEARANCE_HEIGHT_M: Double = 4.0
        const val DEFAULT_MASS_T: Double = 12.0
        const val DEFAULT_WIDTH_M: Double = 2.5
        const val DEFAULT_LENGTH_M: Double = 12.0

        /**
         * Single default Custom Model builder (ATNA-71). Stub and live PathRouters
         * must use this — do not re-hardcode 4.0 m / 12 t literals at call sites.
         */
        fun default(
            preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT,
        ): GraphHopperCustomModelStub = GraphHopperCustomModelStub(
            requiredClearanceHeightM = DEFAULT_CLEARANCE_HEIGHT_M,
            requiredMassT = DEFAULT_MASS_T,
            vehicleWidthM = DEFAULT_WIDTH_M,
            vehicleLengthM = DEFAULT_LENGTH_M,
            preferences = preferences,
        )

        fun from(
            vehicle: Vehicle,
            requiredClearanceHeightM: Double,
            requiredMassT: Double,
            tollPreference: TollPreference = TollPreference.ALLOW_ALL,
            preferences: TruckRoutingPreferences = TruckRoutingPreferences.DEFAULT.copy(
                tollPreference = tollPreference,
            ),
        ): GraphHopperCustomModelStub = GraphHopperCustomModelStub(
            requiredClearanceHeightM = requiredClearanceHeightM,
            requiredMassT = requiredMassT,
            vehicleWidthM = vehicle.widthM,
            vehicleLengthM = vehicle.lengthM,
            preferences = preferences.copy(tollPreference = tollPreference),
        )
    }
}
