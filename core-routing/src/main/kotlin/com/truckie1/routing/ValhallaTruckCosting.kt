package com.truckie1.routing

import com.truckie1.trips.OptimizeMode

/**
 * Builds Valhalla `costing_options.truck` JSON from vehicle Custom Model /
 * [TruckRoutingPreferences] (ATNA-113).
 */
object ValhallaTruckCosting {

    const val COSTING: String = "truck"

    fun optionsJson(model: TruckRoutingModel): String {
        val prefs = model.preferences
        val useTolls = when (prefs.tollPreference) {
            TollPreference.AVOID_ALL -> 0.0
            TollPreference.ALLOW_SOME -> 0.25
            TollPreference.ALLOW_ALL -> 0.5
        }
        val lowClassFactor = when {
            prefs.preferNonResidentialThroughRoutes && prefs.minimiseResidentialPenetration -> 2.5
            prefs.preferNonResidentialThroughRoutes -> 1.5
            else -> 1.0
        }
        val useDistance = when (prefs.optimizeMode) {
            OptimizeMode.CHEAPEST -> 0.6
            else -> 0.0
        }
        return """
        {
          "height": ${model.requiredClearanceHeightM},
          "width": ${model.vehicleWidthM},
          "length": ${model.vehicleLengthM},
          "weight": ${model.requiredMassT},
          "use_tolls": $useTolls,
          "low_class_factor": $lowClassFactor,
          "use_distance": $useDistance
        }
        """.trimIndent()
    }
}
