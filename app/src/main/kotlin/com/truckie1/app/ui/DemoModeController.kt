package com.truckie1.app.ui

import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.Route
import com.truckie1.trips.RouteSession
import com.truckie1.trips.TripSession
import com.truckie1.trips.TripStop
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Owns Demo mode enable/disable: flag flip + hard isolation of vehicles, routes, trips.
 *
 * Call [setEnabled] from Settings. Call [ensureSeededIfDemo] when Home / Vehicles
 * become visible so a flag-on session always has the demo route + vehicle (ATNA-58).
 *
 * Gherkin: features/demo_mode.feature
 */
class DemoModeController(
    private val flags: LocalFeatureFlags,
    private val vehicles: DualModeVehicleProfileRepository,
    private val routeSession: RouteSession,
    private val tripSession: TripSession,
) {
    private var backedUpRoute: Route? = null
    private var backedUpTrips: List<TripStop> = emptyList()
    private var backedUpOptimizeMode = tripSession.lastOptimizeMode
    private var holdingRealBackup: Boolean = false
    private val revisionCounter = AtomicInteger(0)

    /** Bumps whenever demo mode is toggled or re-seeded — UI keys refreshes off this. */
    fun revision(): Int = revisionCounter.get()

    fun setEnabled(enabled: Boolean) {
        val currentlyOn = DemoModeCatalog.isEnabled(flags)
        if (enabled == currentlyOn) {
            if (enabled) {
                ensureSeededIfDemo()
            }
            return
        }

        if (enabled) {
            backedUpRoute = routeSession.activeRoute()
            backedUpTrips = tripSession.stops()
            backedUpOptimizeMode = tripSession.lastOptimizeMode
            holdingRealBackup = true
            DemoModeCatalog.setEnabled(flags, enabled = true)
            applyDemoDataset()
        } else {
            DemoModeCatalog.setEnabled(flags, enabled = false)
            if (holdingRealBackup) {
                routeSession.replaceActive(backedUpRoute)
                tripSession.replaceStops(backedUpTrips)
                tripSession.lastOptimizeMode = backedUpOptimizeMode
            } else if (DemoDataset.isDemoRouteId(routeSession.activeRoute()?.id.orEmpty())) {
                routeSession.replaceActive(null)
                tripSession.replaceStops(emptyList())
                tripSession.lastOptimizeMode = null
            }
            backedUpRoute = null
            backedUpTrips = emptyList()
            backedUpOptimizeMode = null
            holdingRealBackup = false
        }
        revisionCounter.incrementAndGet()
    }

    /**
     * If `ff_demo_mode` is on but the active route is not the demo dataset,
     * seed depot + 6 stops + demo vehicle. Safe to call on every Home bind (ATNA-58).
     */
    fun ensureSeededIfDemo(): Boolean {
        if (!DemoModeCatalog.isEnabled(flags)) return false
        val active = routeSession.activeRoute()
        val needsRoute = active == null || !DemoDataset.isDemoRouteId(active.id)
        val needsVehicle = vehicles.findByRego(DemoDataset.VEHICLE_REGO) == null ||
            vehicles.activeRego?.let { !DemoDataset.isDemoVehicleRego(it) } == true
        if (!needsRoute && !needsVehicle && active != null &&
            active.stops.size == DemoDataset.stops.size
        ) {
            vehicles.ensureDemoVehicleLoaded()
            return false
        }
        if (!holdingRealBackup && needsRoute) {
            // Flag was turned on without going through [setEnabled] — still isolate
            // by backing up whatever is currently active before replacing.
            backedUpRoute = active?.takeUnless { DemoDataset.isDemoRouteId(it.id) }
            backedUpTrips = tripSession.stops().filterNot { stop ->
                DemoDataset.stops.any { it.id == stop.id }
            }
            backedUpOptimizeMode = tripSession.lastOptimizeMode
            holdingRealBackup = backedUpRoute != null || backedUpTrips.isNotEmpty()
        }
        applyDemoDataset()
        revisionCounter.incrementAndGet()
        return true
    }

    private fun applyDemoDataset() {
        vehicles.loadDemoVehicle()
        val demoRoute = DemoDataset.buildRoute(today = LocalDate.now())
        routeSession.replaceActive(demoRoute)
        tripSession.replaceStops(
            demoRoute.stops.map { stop ->
                TripStop(
                    id = stop.id,
                    addressLabel = stop.addressLabel,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                )
            },
        )
        tripSession.lastOptimizeMode = null
    }
}
