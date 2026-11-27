package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteRepository
import com.truckie1.trips.RouteSession
import com.truckie1.trips.RouteStop
import com.truckie1.trips.TripRepository
import com.truckie1.trips.TripSession
import com.truckie1.trips.TripStop
import com.truckie1.trips.OptimizeMode
import java.time.LocalDate

/**
 * One abstraction for vehicles / routes / stops (ATNA-75).
 *
 * Real and Demo implementations share [Vehicle], [Route], and [TripStop] —
 * selecting demo only swaps the backing [AppDataSource], not UI or routing paths.
 *
 * Gherkin: features/demo_mode.feature
 */
interface AppDataSource {
    val vehicles: VehicleProfileRepository
    val routes: RouteRepository
    val trips: TripRepository
}

/** Mutable in-memory / Room-backed stores for real driver data. */
class RealAppDataSource(
    override val vehicles: VehicleProfileRepository,
    override val routes: RouteRepository = RouteSession(),
    override val trips: TripRepository = TripSession(),
) : AppDataSource

/**
 * Pre-seeded demo dataset wrapped as read-only stores.
 * Mutations no-op so dogfood cannot corrupt the sample run.
 */
object DemoAppDataSource {
    fun create(today: LocalDate = LocalDate.now()): AppDataSource {
        val vehicles = InMemoryVehicleProfileRepository()
        vehicles.save(DemoDataset.vehicle)
        vehicles.setActive(DemoDataset.VEHICLE_REGO)

        val routes = RouteSession()
        val demoRoute = DemoDataset.buildRoute(today = today)
        routes.replaceActive(demoRoute)

        val trips = TripSession()
        trips.replaceStops(DemoDataset.buildTripStops())

        return object : AppDataSource {
            override val vehicles: VehicleProfileRepository =
                ReadOnlyVehicleProfileRepository(vehicles)
            override val routes: RouteRepository = ReadOnlyRouteRepository(routes)
            override val trips: TripRepository = ReadOnlyTripRepository(trips)
        }
    }
}

/**
 * Selects Real vs Demo [AppDataSource] from `ff_demo_mode`.
 * Isolation is store swap only — no backup/restore or alternate seed paths.
 */
class DualModeAppDataSource(
    private val real: AppDataSource,
    private val demo: AppDataSource,
    private val flags: FeatureFlags,
) : AppDataSource {
    private fun active(): AppDataSource =
        if (DemoModeCatalog.isEnabled(flags)) demo else real

    override val vehicles: VehicleProfileRepository
        get() = active().vehicles

    override val routes: RouteRepository
        get() = active().routes

    override val trips: TripRepository
        get() = active().trips

    /** Exposed for tests / wiring that need the underlying real store. */
    val realSource: AppDataSource get() = real

    /** Exposed for tests that assert the demo seed without flipping the flag. */
    val demoSource: AppDataSource get() = demo
}

/** No-op write wrapper — demo dataset stays immutable. */
class ReadOnlyVehicleProfileRepository(
    private val inner: VehicleProfileRepository,
) : VehicleProfileRepository {
    override fun save(vehicle: Vehicle) {
        // Demo mode is read-only.
    }

    override fun findByRego(rego: String): Vehicle? = inner.findByRego(rego)

    override fun all(): List<Vehicle> = inner.all()

    override var activeRego: String?
        get() = inner.activeRego
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Demo mode is read-only.
        }

    override fun setActive(rego: String): Boolean = false
}

/** No-op write wrapper for [RouteRepository]. */
class ReadOnlyRouteRepository(
    private val inner: RouteRepository,
) : RouteRepository {
    override fun activeRoute(): Route? = inner.activeRoute()

    override fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate,
        customDate: LocalDate?,
        start: RouteEndpoint?,
        end: RouteEndpoint?,
    ): Route {
        // Keep returning the existing demo route so callers do not NPE.
        return inner.activeRoute()
            ?: error("Demo route repository has no active route")
    }

    override fun addStop(stop: RouteStop) {
        // Demo mode is read-only.
    }

    override fun removeStop(id: String) {
        // Demo mode is read-only.
    }

    override fun clear() {
        // Demo mode is read-only.
    }

    override fun replaceActive(route: Route?) {
        // Demo mode is read-only.
    }
}

/** No-op write wrapper for [TripRepository]. */
class ReadOnlyTripRepository(
    private val inner: TripRepository,
) : TripRepository {
    override var lastOptimizeMode: OptimizeMode?
        get() = inner.lastOptimizeMode
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Demo mode is read-only.
        }

    override fun stops(): List<TripStop> = inner.stops()

    override fun addStop(stop: TripStop) {
        // Demo mode is read-only.
    }

    override fun removeStop(id: String) {
        // Demo mode is read-only.
    }

    override fun moveStop(fromIndex: Int, toIndex: Int) {
        // Demo mode is read-only.
    }

    override fun replaceStops(newStops: List<TripStop>) {
        // Demo mode is read-only.
    }

    override fun updateStop(stop: TripStop) {
        // Demo mode is read-only.
    }
}
