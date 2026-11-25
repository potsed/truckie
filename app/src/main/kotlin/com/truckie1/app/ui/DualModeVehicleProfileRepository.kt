package com.truckie1.app.ui

import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository

/**
 * Hard isolation between real and demo vehicle stores.
 *
 * When [demoModeEnabled] is true, reads/writes hit an in-memory demo store only.
 * When false, they hit the real store only. Demo vehicles never land in [real].
 *
 * Gherkin: features/demo_mode.feature (ATNA-56)
 */
class DualModeVehicleProfileRepository(
    private val real: VehicleProfileRepository,
    private val demo: VehicleProfileRepository = InMemoryVehicleProfileRepository(),
) : VehicleProfileRepository {
    @Volatile
    var demoModeEnabled: Boolean = false

    private fun activeStore(): VehicleProfileRepository =
        if (demoModeEnabled) demo else real

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

    /** Ensures the demo store exposes only [DemoDataset.vehicle] as active. */
    fun loadDemoVehicle() {
        demo.save(DemoDataset.vehicle)
        demo.setActive(DemoDataset.VEHICLE_REGO)
    }
}
