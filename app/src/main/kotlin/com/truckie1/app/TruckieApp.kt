package com.truckie1.app

import androidx.multidex.MultiDexApplication
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreStubEngine
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.routing.StubGraphHopperRouter
import com.truckie1.trips.ClearanceBuffer

class TruckieApp : MultiDexApplication() {
    val featureFlags = LocalFeatureFlags()
    val vehicleProfiles = InMemoryVehicleProfileRepository()
    val mapEngine = MapLibreStubEngine()
    val router = StubGraphHopperRouter()

    /** System clearance buffer (Settings only). Clamped 0.15–1.0 m. */
    var clearanceBufferM: Double = ClearanceBuffer.DEFAULT_M
        set(value) {
            field = ClearanceBuffer.clamp(value)
        }
}
