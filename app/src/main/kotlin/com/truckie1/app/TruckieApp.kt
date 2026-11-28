package com.truckie1.app

import android.app.Application
import com.truckie1.app.profiles.RoomActiveTour
import com.truckie1.app.profiles.RoomVehicleProfileRepository
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.app.settings.DataStoreFeatureFlagStore
import com.truckie1.app.settings.FeatureFlagPersistence
import com.truckie1.app.settings.FeatureFlagStore
import com.truckie1.app.settings.NavigationAppStore
import com.truckie1.app.settings.PackageManagerNavigationAppQuery
import com.truckie1.app.settings.SharedNavigationAppStore
import com.truckie1.app.routing.AsyncRoutingIoCatalog
import com.truckie1.app.routing.PathBlockDetourCatalog
import com.truckie1.app.routing.RoutingBridgePackMapper
import com.truckie1.app.routing.ValhallaLiveCatalog
import com.truckie1.app.trips.RouteNavigateSession
import com.truckie1.app.ui.ActiveTourCatalog
import com.truckie1.app.ui.AuLowBridgesPack
import com.truckie1.app.ui.BridgePackRepository
import com.truckie1.app.ui.CachedBridgePackRepository
import com.truckie1.app.ui.DemoAppDataSource
import com.truckie1.app.ui.DemoModeCatalog
import com.truckie1.app.ui.DemoModeController
import com.truckie1.app.ui.DualModeAppDataSource
import com.truckie1.app.ui.MapRoutePolylineSession
import com.truckie1.app.ui.NavigateMultiStopCatalog
import com.truckie1.app.ui.RealAppDataSource
import com.truckie1.app.ui.RoomRoutePersistCatalog
import com.truckie1.app.ui.RoutePathFromLocationCatalog
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreNativeEngine
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.PathRouter
import com.truckie1.routing.StubPathRouter
import com.truckie1.routing.ValhallaPathRouterFactory
import com.truckie1.trips.ActiveTourFacade
import com.truckie1.trips.ActiveTourSession
import com.truckie1.trips.ClearanceBuffer
import com.truckie1.trips.RouteRepository
import com.truckie1.trips.TripRepository

class TruckieApp : Application() {
    /**
     * Durable via [featureFlagStore] (ATNA-93). Created in [onCreate] after
     * DataStore hydrate — production defaults stay off until seeded / toggled.
     */
    lateinit var featureFlags: LocalFeatureFlags
        private set
    /** Single FeatureFlagStore path — Demo mode and flag overrides share it. */
    lateinit var featureFlagStore: FeatureFlagStore
        private set
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
     * `ff_demo_mode` flips. Same repository contracts for Real and Demo
     * (ATNA-75). Route + TripStop projection share one [ActiveTourFacade]
     * (ATNA-86); real mode persists via Room (ATNA-89).
     */
    val vehicleProfiles: VehicleProfileRepository
        get() = if (::dataSource.isInitialized) dataSource.vehicles else error("TruckieApp not initialised")
    val tripSession: TripRepository
        get() = if (::dataSource.isInitialized) dataSource.trips else bootstrapTour
    val routeSession: RouteRepository
        get() = if (::dataSource.isInitialized) dataSource.routes else bootstrapTour

    /** Placeholder until [onCreate] wires Room-backed ActiveTour + dual mode. */
    private val bootstrapTour: ActiveTourFacade = ActiveTourSession()

    /**
     * Navigate session shares the app [routeOptimizer] / PathRouter (ATNA-71/87)
     * via ActiveTourPath — never a start→end-only stub that skips stops.
     */
    val routeNavigateSession = RouteNavigateSession(composerProvider = { routeOptimizer })
    /** Last Optimise tour path for home map polyline (`ff_map_route_polyline`). */
    val mapRoutePolylineSession = MapRoutePolylineSession()
    val mapEngine = MapLibreNativeEngine()

    /** Valhalla base URL / Simple Routing default when API key set. */
    var valhallaBaseUrl: String? = ValhallaLiveCatalog.effectiveBaseUrl().ifBlank { null }

    /** Hosted Valhalla Bearer token (BuildConfig / secrets). Blank for self-host. */
    var valhallaApiKey: String? = ValhallaLiveCatalog.configuredApiKey().ifBlank { null }

    /**
     * Single app-owned PathRouter: live Valhalla (`ff_valhalla_live` +
     * credentials) or stub. Gated by async routing I/O (ATNA-88). Injected
     * into Navigate **and** Optimise. Sole live engine — no GraphHopper path.
     */
    fun pathRouter(): PathRouter {
        if (AsyncRoutingIoCatalog.allowsLiveValhalla(featureFlags) &&
            ValhallaLiveCatalog.isLivePathRouterActive(featureFlags)
        ) {
            return ValhallaPathRouterFactory.create(
                liveEnabled = true,
                baseUrl = valhallaBaseUrl ?: ValhallaLiveCatalog.effectiveBaseUrl(),
                apiKey = valhallaApiKey ?: ValhallaLiveCatalog.configuredApiKey().ifBlank { null },
            )
        }
        return StubPathRouter()
    }

    /**
     * Multi-stop tour optimiser (ATNA-68/71/102). Same [pathRouter] as Navigate;
     * pack bridges → routing constraints when `ff_bridge_passage` is on;
     * PathBlockDetour retries PathRouter around geographic blocks.
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
                pathBlockDetourEnabled = PathBlockDetourCatalog.isEnabled(featureFlags),
            )
        }

    /** System clearance buffer (Settings only). Clamped 0.15–1.0 m. */
    var clearanceBufferM: Double = ClearanceBuffer.DEFAULT_M
        set(value) {
            field = ClearanceBuffer.clamp(value)
        }

    override fun onCreate() {
        super.onCreate()
        // ATNA-93: hydrate durable flag overrides (Demo mode, heading-up, …).
        featureFlagStore = DataStoreFeatureFlagStore(this)
        featureFlags = FeatureFlagPersistence.bind(featureFlagStore)
        val db = VehicleDatabase.build(this)
        // Structural defaults — seedIfAbsent so persisted user toggles win and
        // seeds do not write the DataStore (production defaults stay off).
        featureFlags.seedIfAbsent(FlagKeys.ACTIVE_TOUR, ActiveTourCatalog.DEFAULT_ENABLED)
        featureFlags.seedIfAbsent(
            FlagKeys.NAVIGATE_MULTI_STOP,
            NavigateMultiStopCatalog.DEFAULT_ENABLED,
        )
        featureFlags.seedIfAbsent(
            FlagKeys.ROUTE_PATH_FROM_LOCATION,
            RoutePathFromLocationCatalog.DEFAULT_ENABLED,
        )
        featureFlags.seedIfAbsent(
            FlagKeys.ASYNC_ROUTING_IO,
            AsyncRoutingIoCatalog.DEFAULT_ENABLED,
        )
        featureFlags.seedIfAbsent(
            FlagKeys.ROOM_ROUTE_PERSIST,
            RoomRoutePersistCatalog.DEFAULT_ENABLED,
        )
        featureFlags.seedIfAbsent(
            FlagKeys.PATH_BLOCK_DETOUR,
            PathBlockDetourCatalog.DEFAULT_ENABLED,
        )
        val realTour: ActiveTourFacade = RoomActiveTour(db.activeTourDao())
        // ATNA-75: Real (Room vehicles + Room tour) vs Demo (read-only seed).
        dataSource = DualModeAppDataSource(
            real = RealAppDataSource(
                vehicles = RoomVehicleProfileRepository(db.vehicleDao()),
                tour = realTour,
            ),
            demo = DemoAppDataSource.create(),
            flags = featureFlags,
        )
        demoModeController = DemoModeController(flags = featureFlags)
        navigationAppStore = SharedNavigationAppStore(this)
        navigationAppQuery = PackageManagerNavigationAppQuery(packageManager)
        // Dogfood: enable shipped capabilities in debug builds only.
        // seedIfAbsent — never clobber persisted Demo / heading-up overrides.
        if (BuildConfig.DEBUG) {
            featureFlags.seedIfAbsent(FlagKeys.VEHICLE_PROFILES, true)
            featureFlags.seedIfAbsent(FlagKeys.MULTI_STOP, true)
            featureFlags.seedIfAbsent(FlagKeys.STOP_CARGO, true)
            featureFlags.seedIfAbsent(FlagKeys.ROUTE_OPTIMIZE, true)
            featureFlags.seedIfAbsent(FlagKeys.NAV_CHROME, true)
            featureFlags.seedIfAbsent(FlagKeys.MAP_HOME, true)
            featureFlags.seedIfAbsent(FlagKeys.ADDRESS_AUTOCOMPLETE, true)
            featureFlags.seedIfAbsent(FlagKeys.GNAF_ADDRESS_GEOCODE, true)
            featureFlags.seedIfAbsent(FlagKeys.MAP_LOCATION_PUCK, true)
            featureFlags.seedIfAbsent(FlagKeys.MAP_HEADING_UP, true)
            featureFlags.seedIfAbsent(FlagKeys.MAP_STREET_LABELS, true)
            featureFlags.seedIfAbsent(FlagKeys.ROUTE_START_END, true)
            featureFlags.seedIfAbsent(FlagKeys.NAVIGATE, true)
            featureFlags.seedIfAbsent(FlagKeys.BRIDGE_MARKERS, true)
            featureFlags.seedIfAbsent(FlagKeys.STOP_MARKERS, true)
            featureFlags.seedIfAbsent(FlagKeys.MAP_ROUTE_POLYLINE, true)
            featureFlags.seedIfAbsent(
                FlagKeys.VALHALLA_LIVE,
                ValhallaLiveCatalog.ENABLED_BY_DEFAULT_IN_DEBUG,
            )
            featureFlags.seedIfAbsent(FlagKeys.PATH_BLOCK_DETOUR, true)
            featureFlags.seedIfAbsent(FlagKeys.NEXT_DESTINATION_DISPLAY, true)
            featureFlags.seedIfAbsent(FlagKeys.VERIFIED_CBD_CLEARANCES, true)
            featureFlags.seedIfAbsent(FlagKeys.QUEENSLAND_RAIL_CLEARANCES, true)
            featureFlags.seedIfAbsent(FlagKeys.W5_8_LOW_ONLY, true)
            featureFlags.seedIfAbsent(FlagKeys.TMR_STRUCTURE_CLEARANCES, true)
            featureFlags.seedIfAbsent(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS, true)
            featureFlags.seedIfAbsent(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, true)
            featureFlags.seedIfAbsent(FlagKeys.BRIDGE_PASSAGE, true)
            featureFlags.seedIfAbsent(FlagKeys.AU_LOW_BRIDGES_PACK, true)
            featureFlags.seedIfAbsent(FlagKeys.OWNED_ADDRESS_PACK, true)
            // Demo mode stays off unless persisted / Settings opt-in.
            if (DemoModeCatalog.ENABLED_BY_DEFAULT_IN_DEBUG) {
                featureFlags.seedIfAbsent(FlagKeys.DEMO_MODE, true)
            }
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
