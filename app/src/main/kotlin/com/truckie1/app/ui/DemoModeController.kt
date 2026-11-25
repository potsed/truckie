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
    private val revisionCounter = AtomicInteger(0)

    /** Bumps whenever demo mode is toggled — UI can key refreshes off this. */
    fun revision(): Int = revisionCounter.get()

    fun setEnabled(enabled: Boolean) {
        val currentlyOn = DemoModeCatalog.isEnabled(flags)
        if (enabled == currentlyOn && enabled == vehicles.demoModeEnabled) {
            if (enabled) {
                // Idempotent re-apply of demo dataset (e.g. after process restore).
                applyDemoDataset()
            }
            return
        }

        if (enabled) {
            backedUpRoute = routeSession.activeRoute()
            backedUpTrips = tripSession.stops()
            backedUpOptimizeMode = tripSession.lastOptimizeMode
            DemoModeCatalog.setEnabled(flags, enabled = true)
            vehicles.demoModeEnabled = true
            applyDemoDataset()
        } else {
            DemoModeCatalog.setEnabled(flags, enabled = false)
            vehicles.demoModeEnabled = false
            routeSession.replaceActive(backedUpRoute)
            tripSession.replaceStops(backedUpTrips)
            tripSession.lastOptimizeMode = backedUpOptimizeMode
            backedUpRoute = null
            backedUpTrips = emptyList()
            backedUpOptimizeMode = null
        }
        revisionCounter.incrementAndGet()
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
