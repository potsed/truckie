package com.truckie1.routing

import com.truckie1.profiles.Vehicle

/**
 * Stub GraphHopper Custom Model built from active vehicle + trip constraints.
 * Toll preference defaults to [TollPreference.ALLOW_ALL] (avoid-none / allow-all passthrough).
 */
data class GraphHopperCustomModelStub(
    val requiredClearanceHeightM: Double,
    val requiredMassT: Double,
    val vehicleWidthM: Double,
    val vehicleLengthM: Double,
    val tollPreference: TollPreference = TollPreference.ALLOW_ALL,
) {
    fun toRequestSnippet(): String =
        """
        {
          "priority": [{ "if": "max_height < $requiredClearanceHeightM", "multiply_by": "0" },
                       { "if": "max_weight < $requiredMassT", "multiply_by": "0" }],
          "areas": {},
          "toll": "${tollPreference.name}"
        }
        """.trimIndent()

    companion object {
        fun from(
            vehicle: Vehicle,
            requiredClearanceHeightM: Double,
            requiredMassT: Double,
            tollPreference: TollPreference = TollPreference.ALLOW_ALL,
        ): GraphHopperCustomModelStub = GraphHopperCustomModelStub(
            requiredClearanceHeightM = requiredClearanceHeightM,
            requiredMassT = requiredMassT,
            vehicleWidthM = vehicle.widthM,
            vehicleLengthM = vehicle.lengthM,
            tollPreference = tollPreference,
        )
    }
}
