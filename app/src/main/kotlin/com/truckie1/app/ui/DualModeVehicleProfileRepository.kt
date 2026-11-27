package com.truckie1.app.ui

import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository

/**
 * Thin vehicle-only view of [DualModeAppDataSource] for call sites that still
 * inject [VehicleProfileRepository] (ATNA-75). Prefer the full [AppDataSource].
 *
 * Isolation follows `ff_demo_mode` via [dataSource] — no separate latch, no
 * auto-seed on read (demo store is pre-seeded and read-only).
 *
 * Gherkin: features/demo_mode.feature
 */
class DualModeVehicleProfileRepository(
    private val dataSource: DualModeAppDataSource,
) : VehicleProfileRepository {
    private fun active(): VehicleProfileRepository = dataSource.vehicles

    override fun save(vehicle: Vehicle) {
        active().save(vehicle)
    }

    override fun findByRego(rego: String): Vehicle? = active().findByRego(rego)

    override fun all(): List<Vehicle> = active().all()

    override var activeRego: String?
        get() = active().activeRego
        set(value) {
            active().activeRego = value
        }

    override fun setActive(rego: String): Boolean = active().setActive(rego)
}
