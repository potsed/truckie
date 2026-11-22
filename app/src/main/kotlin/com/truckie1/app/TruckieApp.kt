package com.truckie1.app

import androidx.multidex.MultiDexApplication
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreStubEngine
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.routing.StubGraphHopperRouter
import com.truckie1.trips.ClearanceBuffer

class TruckieApp : MultiDexApplication() {
    val featureFlags = LocalFeatureFlags()
    lateinit var vehicleProfiles: VehicleProfileRepository
        private set
    val mapEngine = MapLibreStubEngine()
    val router = StubGraphHopperRouter()

    /** System clearance buffer (Settings only). Clamped 0.15–1.0 m. */
    var clearanceBufferM: Double = ClearanceBuffer.DEFAULT_M
        set(value) {
            field = ClearanceBuffer.clamp(value)
        }

    override fun onCreate() {
        super.onCreate()
        val db = VehicleDatabase.build(this)
        vehicleProfiles = RoomVehicleProfileRepository(db.vehicleDao())
        // Dogfood: enable vehicle profiles in debug builds only.
        if (BuildConfig.DEBUG) {
            featureFlags.setEnabled(com.truckie1.flags.FlagKeys.VEHICLE_PROFILES, true)
        }
    }
}
