package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository
import com.truckie1.trips.ActiveTourSession
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteRepository
import com.truckie1.trips.RouteStop
import com.truckie1.trips.TripRepository
import com.truckie1.trips.TripStop
import java.time.LocalDate

/**
 * One abstraction for vehicles / routes / stops (ATNA-75 + ATNA-86).
 *
 * Real and Demo share [Vehicle], [Route], and projected [TripStop] — selecting
 * demo only swaps the backing [AppDataSource], not UI or routing paths.
 *
 * [routes] and [trips] are always the same [ActiveTourSession] (single SoT).
 * [TripStop] is a projection via [com.truckie1.trips.TripStopProjection].
 *
 * Gherkin: features/demo_mode.feature, features/active_tour.feature
 */
interface AppDataSource {
    val vehicles: VehicleProfileRepository
    val routes: RouteRepository
    val trips: TripRepository
}

/**
 * Real driver stores: Room vehicles + one in-memory [ActiveTourSession] for
 * map-home Route and Plan Trip / Optimise projections.
 */
class RealAppDataSource(
    override val vehicles: VehicleProfileRepository,
    val tour: ActiveTourSession = ActiveTourSession(),
) : AppDataSource {
    override val routes: RouteRepository get() = tour
    override val trips: TripRepository get() = tour
}

/**
 * Pre-seeded demo dataset wrapped as read-only stores.
 * One [ActiveTourSession] seeds Route (+ cargo); TripStop is projected.
 * [ReadOnlyActiveTour] is a single facade instance for both [routes] and [trips].
 */
object DemoAppDataSource {
    fun create(today: LocalDate = LocalDate.now()): AppDataSource {
        val vehicles = InMemoryVehicleProfileRepository()
        vehicles.save(DemoDataset.vehicle)
        vehicles.setActive(DemoDataset.VEHICLE_REGO)

        val tour = ActiveTourSession()
        tour.replaceActive(DemoDataset.buildRoute(today = today))
        val readOnlyTour = ReadOnlyActiveTour(tour)

        return object : AppDataSource {
            override val vehicles: VehicleProfileRepository =
                ReadOnlyVehicleProfileRepository(vehicles)
            override val routes: RouteRepository = readOnlyTour
            override val trips: TripRepository = readOnlyTour
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

/**
 * Read-only active-tour facade for demo: implements both [RouteRepository] and
 * [TripRepository] so [AppDataSource.routes] === [AppDataSource.trips].
 */
class ReadOnlyActiveTour(
    private val inner: ActiveTourSession,
) : RouteRepository, TripRepository {
    override fun activeRoute(): Route? = inner.activeRoute()

    override fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate,
        customDate: LocalDate?,
        start: RouteEndpoint?,
        end: RouteEndpoint?,
    ): Route {
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

    override var lastOptimizeMode: OptimizeMode?
        get() = inner.lastOptimizeMode
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Demo mode is read-only.
        }

    override fun stops(): List<TripStop> = inner.stops()

    override fun addStop(stop: TripStop) {
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
