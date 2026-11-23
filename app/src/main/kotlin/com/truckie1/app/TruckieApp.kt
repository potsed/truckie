package com.truckie1.app

import android.app.Application
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.NavigationAppStore
import com.truckie1.app.settings.PackageManagerNavigationAppQuery
import com.truckie1.app.settings.SharedNavigationAppStore
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.BridgePackRepository
import com.truckie1.app.ui.CachedBridgePackRepository
import com.truckie1.app.ui.NoOpBridgePackDownloader
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreNativeEngine
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.routing.DefaultTourCandidates
import com.truckie1.routing.StubGraphHopperRouter
import com.truckie1.routing.VehicleTripOptimizer
import com.truckie1.trips.ClearanceBuffer
import com.truckie1.trips.RouteSession
import com.truckie1.trips.TripSession

class TruckieApp : Application() {
    val featureFlags = LocalFeatureFlags()
    lateinit var vehicleProfiles: VehicleProfileRepository
        private set
    lateinit var navigationAppStore: NavigationAppStore
        private set
    lateinit var navigationAppQuery: PackageManagerNavigationAppQuery
        private set
    lateinit var bridgePackRepository: BridgePackRepository
        private set
    val tripSession = TripSession()
    val routeSession = RouteSession()
    val routeNavigateSession = RouteNavigateSession()
    val mapEngine = MapLibreNativeEngine()
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
        navigationAppStore = SharedNavigationAppStore(this)
        navigationAppQuery = PackageManagerNavigationAppQuery(packageManager)
        // AU bridge pack: local-first (bundled sample → disk cache). loadActive
        // never networks; refresh only when a newer remote version is advertised.
        // Remote host not wired yet — NoOp until CDN/NHVR pack URL (ATNA-11).
        bridgePackRepository = CachedBridgePackRepository(
            context = this,
            downloader = NoOpBridgePackDownloader,
        )
        // Dogfood: enable shipped capabilities in debug builds only.
        if (BuildConfig.DEBUG) {
            featureFlags.setEnabled(FlagKeys.VEHICLE_PROFILES, true)
            featureFlags.setEnabled(FlagKeys.MULTI_STOP, true)
            featureFlags.setEnabled(FlagKeys.STOP_CARGO, true)
            featureFlags.setEnabled(FlagKeys.ROUTE_OPTIMIZE, true)
            featureFlags.setEnabled(FlagKeys.NAV_CHROME, true)
            featureFlags.setEnabled(FlagKeys.MAP_HOME, true)
            featureFlags.setEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE, true)
            featureFlags.setEnabled(FlagKeys.MAP_LOCATION_PUCK, true)
            featureFlags.setEnabled(FlagKeys.ROUTE_START_END, true)
            featureFlags.setEnabled(FlagKeys.NAVIGATE, true)
            featureFlags.setEnabled(FlagKeys.BRIDGE_MARKERS, true)
            // Verified CBD clearances: no mock heights (ATNA-42).
            featureFlags.setEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES, true)
            // Queensland Rail SEQ road-under-rail clearances (ATNA-44).
            featureFlags.setEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES, true)
            // Demo mode: temporary dogfood affordances (Jump to sample bridges).
            featureFlags.setEnabled(FlagKeys.DEMO_MODE, true)
        }
    }
}
