package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.OptimizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin / architecture: truck routing prefers bridges, weight, tolls,
 * residential avoidance, then Fastest/Cheapest among safe tours
 * (Custom Model API shape; Phase D live).
 */
class TruckRoutingPreferencesTest {

    @Test
    fun defaultsAvoidBridgesWeightSoftResidentialPreferFastest() {
        val prefs = TruckRoutingPreferences.DEFAULT
        assertTrue(prefs.avoidLowBridges)
        assertTrue(prefs.avoidWeightRestrictedRoads)
        assertTrue(prefs.preferNonResidentialThroughRoutes)
        assertTrue(prefs.minimiseResidentialPenetration)
        assertEquals(TollPreference.ALLOW_ALL, prefs.tollPreference)
        assertEquals(OptimizeMode.FASTEST, prefs.optimizeMode)
    }

    @Test
    fun customModelSnippetEncodesSoftResidentialMinimisePenetration() {
        val model = TruckRoutingModel(
            requiredClearanceHeightM = 4.3,
            requiredMassT = 16.0,
            vehicleWidthM = 2.5,
            vehicleLengthM = 12.5,
            preferences = TruckRoutingPreferences.DEFAULT,
        )
        val snippet = model.toRequestSnippet()
        assertTrue(snippet.contains("max_height"))
        assertTrue(snippet.contains("max_weight"))
        assertTrue(snippet.contains("RESIDENTIAL"))
        // Soft preference with stronger deprioritisation when minimising penetration;
        // never multiply_by 0 (hard block) — delivery access stays OK.
        assertTrue(snippet.contains("""road_class == RESIDENTIAL", "multiply_by": "0.05""""))
        assertFalse(snippet.contains("""road_class == RESIDENTIAL", "multiply_by": "0""""))
        assertTrue(snippet.contains("\"prefer_non_residential_through_routes\": true"))
        assertTrue(snippet.contains("\"minimise_residential_penetration\": true"))
        assertTrue(snippet.contains("\"residential_soft_preference_only\": true"))
        assertTrue(snippet.contains("\"residential_delivery_access_ok\": true"))
        assertTrue(snippet.contains("\"toll\": \"ALLOW_ALL\""))
        assertTrue(snippet.contains("\"optimize\": \"FASTEST\""))
        assertTrue(snippet.contains("\"block_under_low_clearance\": true"))
        assertTrue(snippet.contains("\"over_deck_ok\": true"))
        assertTrue(model.preferNonResidentialThroughRoutes)
    }

    @Test
    fun cheapestOptimizeModeEncodedInSnippet() {
        val prefs = TruckRoutingPreferences.DEFAULT.copy(optimizeMode = OptimizeMode.CHEAPEST)
        val model = TruckRoutingModel(
            requiredClearanceHeightM = 4.0,
            requiredMassT = 10.0,
            vehicleWidthM = 2.4,
            vehicleLengthM = 10.0,
            preferences = prefs,
        )
        assertTrue(model.toRequestSnippet().contains("\"optimize\": \"CHEAPEST\""))
    }

    @Test
    fun softResidentialPreferenceCanBeDisabled() {
        val prefs = TruckRoutingPreferences.DEFAULT.copy(preferNonResidentialThroughRoutes = false)
        val model = TruckRoutingModel(
            requiredClearanceHeightM = 4.0,
            requiredMassT = 10.0,
            vehicleWidthM = 2.4,
            vehicleLengthM = 10.0,
            preferences = prefs,
        )
        assertFalse(model.toRequestSnippet().contains("RESIDENTIAL"))
        assertTrue(model.toRequestSnippet().contains("\"prefer_non_residential_through_routes\": false"))
    }

    @Test
    fun fromVehicleKeepsSoftResidentialDefault() {
        val vehicle = Vehicle(
            rego = "ABC123",
            nickname = "Rig",
            vehicleType = "Rigid",
            make = "Isuzu",
            model = "FSR",
            year = 2020,
            heightM = 3.5,
            widthM = 2.5,
            lengthM = 12.0,
            massT = 8.0,
            tareT = 6.0,
            gvmT = 16.0,
            gcmT = 25.0,
            axleCount = 2,
            networkClass = "GML",
        )
        val model = TruckRoutingModel.from(
            vehicle = vehicle,
            requiredClearanceHeightM = 3.65,
            requiredMassT = 16.5,
        )
        assertTrue(model.preferences.preferNonResidentialThroughRoutes)
        assertTrue(model.toRequestSnippet().contains("\"residential_soft_preference_only\": true"))
    }
}
