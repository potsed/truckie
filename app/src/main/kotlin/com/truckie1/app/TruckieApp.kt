package com.truckie1.app

import android.app.Application
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.NavigationAppStore
import com.truckie1.app.settings.PackageManagerNavigationAppQuery
import com.truckie1.app.settings.SharedNavigationAppStore
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.AuLowBridgesPack
import com.truckie1.app.ui.BridgePackRepository
import com.truckie1.app.ui.CachedBridgePackRepository
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
            // Course-up while following — map orients to driving direction.
            featureFlags.setEnabled(FlagKeys.MAP_HEADING_UP, true)
            featureFlags.setEnabled(FlagKeys.ROUTE_START_END, true)
            featureFlags.setEnabled(FlagKeys.NAVIGATE, true)
            featureFlags.setEnabled(FlagKeys.BRIDGE_MARKERS, true)
            // Verified CBD clearances: no mock heights (ATNA-42).
            featureFlags.setEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES, true)
            // Queensland Rail SEQ road-under-rail clearances (ATNA-44).
            featureFlags.setEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES, true)
            // W5-8 only for verified low / QR underbridges (ATNA-45).
            featureFlags.setEnabled(FlagKeys.W5_8_LOW_ONLY, true)
            // TMR Structure Clearances + BCC road-restrictions GIS (ATNA-46).
            featureFlags.setEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES, true)
            featureFlags.setEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS, true)
            // Home-map conflicts only for active vehicle height + buffer (ATNA-47).
            featureFlags.setEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, true)
            // Over vs under passage — conflicts block under-paths only (ATNA-48).
            featureFlags.setEnabled(FlagKeys.BRIDGE_PASSAGE, true)
            // OSS AU low-bridges pack refresh (ATNA-50) — local-first when newer.
            featureFlags.setEnabled(FlagKeys.AU_LOW_BRIDGES_PACK, true)
            // Demo mode stays off by default (even in debug) — opt-in via Settings.
            // Do not set FlagKeys.DEMO_MODE here; see DemoModeCatalog.ENABLED_BY_DEFAULT_IN_DEBUG.
        }
        // AU bridge pack: local-first (bundled sample → disk cache). loadActive
        // never networks; refresh only when a newer remote version is advertised.
        // ff_au_low_bridges_pack → HTTPS GitHub Release asset (shared OSS schema).
        bridgePackRepository = CachedBridgePackRepository(
            context = this,
            downloader = AuLowBridgesPack.downloaderFor(featureFlags),
        )
        if (AuLowBridgesPack.isEnabled(featureFlags)) {
            // Best-effort off-main refresh; failures keep on-device fallback.
            Thread(
                {
                    runCatching {
                        bridgePackRepository.refresh(AuLowBridgesPack.remoteManifest())
                    }
                },
                "au-low-bridges-pack-refresh",
            ).apply { isDaemon = true }.start()
        }
    }
}
