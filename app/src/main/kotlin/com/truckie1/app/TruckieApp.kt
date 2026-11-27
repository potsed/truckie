package com.truckie1.app

import android.app.Application
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.NavigationAppStore
import com.truckie1.app.settings.PackageManagerNavigationAppQuery
import com.truckie1.app.settings.SharedNavigationAppStore
import com.truckie1.app.routing.RoutingBridgePackMapper
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.AuLowBridgesPack
import com.truckie1.app.ui.BridgePackRepository
import com.truckie1.app.ui.CachedBridgePackRepository
import com.truckie1.app.ui.DemoAppDataSource
import com.truckie1.app.ui.DemoModeController
import com.truckie1.app.ui.DualModeAppDataSource
import com.truckie1.app.ui.MapRoutePolylineSession
import com.truckie1.app.ui.RealAppDataSource
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreNativeEngine
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.routing.GraphHopperPathRouterFactory
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.PathRouter
import com.truckie1.trips.ClearanceBuffer
import com.truckie1.trips.RouteRepository
import com.truckie1.trips.RouteSession
import com.truckie1.trips.TripRepository
import com.truckie1.trips.TripSession

class TruckieApp : Application() {
    val featureFlags = LocalFeatureFlags()
    lateinit var dataSource: DualModeAppDataSource
        private set
    lateinit var demoModeController: DemoModeController
        private set
    lateinit var navigationAppStore: NavigationAppStore
        private set
    lateinit var navigationAppQuery: PackageManagerNavigationAppQuery
        private set
    lateinit var bridgePackRepository: BridgePackRepository
        private set

    /**
     * Active vehicle/route/trip stores — swap with [dataSource] when
     * `ff_demo_mode` flips. Same repository contracts for Real and Demo (ATNA-75).
     */
    val vehicleProfiles: VehicleProfileRepository
        get() = if (::dataSource.isInitialized) dataSource.vehicles else error("TruckieApp not initialised")
    val tripSession: TripRepository
        get() = if (::dataSource.isInitialized) dataSource.trips else bootstrapTrips
    val routeSession: RouteRepository
        get() = if (::dataSource.isInitialized) dataSource.routes else bootstrapRoutes

    /** Real stores held until [onCreate] wires Room + dual mode. */
    private val bootstrapRoutes = RouteSession()
    private val bootstrapTrips = TripSession()

    /**
     * Navigate session shares the app [pathRouter] factory (ATNA-71) — never a
     * hard-wired Stub that ignores `ff_graphhopper_live`.
     */
    val routeNavigateSession = RouteNavigateSession(pathRouterProvider = { pathRouter() })
    /** Last Optimise tour path for home map polyline (`ff_map_route_polyline`). */
    val mapRoutePolylineSession = MapRoutePolylineSession()
    val mapEngine = MapLibreNativeEngine()

    /**
     * Optional live GraphHopper base URL (no trailing path). Empty / null → stub
     * even when [FlagKeys.GRAPHHOPPER_LIVE] is on.
     */
    var graphHopperBaseUrl: String? = null

    /**
     * Single app-owned PathRouter: stub vs live from `ff_graphhopper_live` +
     * [graphHopperBaseUrl]. Injected into Navigate **and** Optimise.
     */
    fun pathRouter(): PathRouter = GraphHopperPathRouterFactory.create(
        liveEnabled = featureFlags.isEnabled(FlagKeys.GRAPHHOPPER_LIVE),
        baseUrl = graphHopperBaseUrl,
    )

    /**
     * Multi-stop GraphHopper tour optimiser (ATNA-68/71). Same [pathRouter] as
     * Navigate; pack bridges → routing constraints when `ff_bridge_passage` is
     * on (additive composer args — passage UI policy owned by sibling).
     */
    val routeOptimizer: MultiStopTourComposer
        get() {
            val passageOn = featureFlags.isEnabled(FlagKeys.BRIDGE_PASSAGE)
            val packBridges = if (::bridgePackRepository.isInitialized) {
                bridgePackRepository.loadActive().bridges
            } else {
                emptyList()
            }
            return MultiStopTourComposer(
                pathRouter = pathRouter(),
                bridges = RoutingBridgePackMapper.bridgesForComposer(
                    packBridges = packBridges,
                    bridgePassageEnabled = passageOn,
                ),
                bridgePassageEnabled = passageOn,
            )
        }

    /** System clearance buffer (Settings only). Clamped 0.15–1.0 m. */
    var clearanceBufferM: Double = ClearanceBuffer.DEFAULT_M
        set(value) {
            field = ClearanceBuffer.clamp(value)
        }

    override fun onCreate() {
        super.onCreate()
        val db = VehicleDatabase.build(this)
        // ATNA-75: one AppDataSource abstraction — Real (Room + sessions) vs
        // Demo (pre-seeded read-only). Flag selects the store; same domain types.
        dataSource = DualModeAppDataSource(
            real = RealAppDataSource(
                vehicles = RoomVehicleProfileRepository(db.vehicleDao()),
                routes = bootstrapRoutes,
                trips = bootstrapTrips,
            ),
            demo = DemoAppDataSource.create(),
            flags = featureFlags,
        )
        demoModeController = DemoModeController(flags = featureFlags)
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
            // Prefer street names over bus-stop / POI clutter (ATNA-78).
            featureFlags.setEnabled(FlagKeys.MAP_STREET_LABELS, true)
            featureFlags.setEnabled(FlagKeys.ROUTE_START_END, true)
            featureFlags.setEnabled(FlagKeys.NAVIGATE, true)
            featureFlags.setEnabled(FlagKeys.BRIDGE_MARKERS, true)
            // Route stop markers: pickup vs drop-off on MapLibre home (ATNA-81).
            featureFlags.setEnabled(FlagKeys.STOP_MARKERS, true)
            // Constrained PathRouter route line on MapLibre home (ATNA-82).
            featureFlags.setEnabled(FlagKeys.MAP_ROUTE_POLYLINE, true)
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
