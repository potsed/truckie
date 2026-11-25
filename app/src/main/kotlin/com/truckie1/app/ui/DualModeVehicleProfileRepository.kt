package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository

/**
 * Hard isolation between real and demo vehicle stores (ATNA-56 / ATNA-58).
 *
 * **Source of truth is `ff_demo_mode`**, not a separate mutable latch — so a Settings
 * toggle that flips the flag always hides real vehicles (and vice versa), even if
 * seeding of the demo route is handled separately by [DemoModeController].
 *
 * Gherkin: features/demo_mode.feature
 */
class DualModeVehicleProfileRepository(
    private val real: VehicleProfileRepository,
    private val flags: FeatureFlags,
    private val demo: VehicleProfileRepository = InMemoryVehicleProfileRepository(),
) : VehicleProfileRepository {
    private fun useDemo(): Boolean = DemoModeCatalog.isEnabled(flags)

    private fun activeStore(): VehicleProfileRepository {
        if (useDemo()) {
            ensureDemoVehicleLoaded()
            return demo
        }
        return real
    }

    /** Seeds the in-memory demo store when empty so `all()` is never an empty demo gap. */
    fun ensureDemoVehicleLoaded() {
        if (demo.findByRego(DemoDataset.VEHICLE_REGO) == null) {
            demo.save(DemoDataset.vehicle)
        }
        if (demo.activeRego == null || !DemoDataset.isDemoVehicleRego(demo.activeRego!!)) {
            demo.setActive(DemoDataset.VEHICLE_REGO)
        }
    }

    /** @deprecated Prefer flag-driven [useDemo]; kept for tests that flip isolation explicitly. */
    @Deprecated("Isolation follows ff_demo_mode", ReplaceWith("DemoModeCatalog.isEnabled(flags)"))
    var demoModeEnabled: Boolean
        get() = useDemo()
        set(@Suppress("UNUSED_PARAMETER") value) {
            // No-op: flag is the source of truth (ATNA-58).
        }

    override fun save(vehicle: Vehicle) {
        activeStore().save(vehicle)
    }

    override fun findByRego(rego: String): Vehicle? = activeStore().findByRego(rego)

    override fun all(): List<Vehicle> = activeStore().all()

    override var activeRego: String?
        get() = activeStore().activeRego
        set(value) {
            activeStore().activeRego = value
        }

    override fun setActive(rego: String): Boolean = activeStore().setActive(rego)

    fun loadDemoVehicle() {
        demo.save(DemoDataset.vehicle)
        demo.setActive(DemoDataset.VEHICLE_REGO)
    }
}
