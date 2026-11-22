package com.truckie1.app

import android.app.Application
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreStubEngine
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.routing.DefaultTourCandidates
import com.truckie1.routing.StubGraphHopperRouter
import com.truckie1.routing.VehicleTripOptimizer
import com.truckie1.trips.ClearanceBuffer
import com.truckie1.trips.TripSession

class TruckieApp : Application() {
    val featureFlags = LocalFeatureFlags()
    lateinit var vehicleProfiles: VehicleProfileRepository
        private set
    val tripSession = TripSession()
    val mapEngine = MapLibreStubEngine()
    val router = StubGraphHopperRouter()

    /**
     * Stub optimizer: GraphHopper Custom Model constraints applied via SafeRouteSelector.
     * Candidate duration/cost vary with stop order so Fastest/Cheapest can reorder.
     */
    val routeOptimizer = VehicleTripOptimizer(
        candidatesForOrder = { orderedStops ->
            val orderKey = orderedStops.map { it.id }.joinToString().hashCode().and(0xff)
            DefaultTourCandidates.forStops(orderedStops.size, orderKey = orderKey)
        },
    )

    /** System clearance buffer (Settings only). Clamped 0.15–1.0 m. */
    var clearanceBufferM: Double = ClearanceBuffer.DEFAULT_M
        set(value) {
            field = ClearanceBuffer.clamp(value)
        }

    override fun onCreate() {
        super.onCreate()
        val db = VehicleDatabase.build(this)
        vehicleProfiles = RoomVehicleProfileRepository(db.vehicleDao())
        // Dogfood: enable shipped capabilities in debug builds only.
        if (BuildConfig.DEBUG) {
            featureFlags.setEnabled(FlagKeys.VEHICLE_PROFILES, true)
            featureFlags.setEnabled(FlagKeys.MULTI_STOP, true)
            featureFlags.setEnabled(FlagKeys.STOP_CARGO, true)
            featureFlags.setEnabled(FlagKeys.ROUTE_OPTIMIZE, true)
            featureFlags.setEnabled(FlagKeys.NAV_CHROME, true)
        }
    }
}
