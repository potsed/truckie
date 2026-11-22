package com.truckie1.app.profiles

import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository

/**
 * Room-backed vehicle profiles keyed by normalized rego.
 */
class RoomVehicleProfileRepository(
    private val dao: VehicleDao,
) : VehicleProfileRepository {
    override var activeRego: String?
        get() = dao.activeRego()
        set(value) {
            if (value == null) {
                dao.clearActive()
            } else {
                setActive(value)
            }
        }

    override fun save(vehicle: Vehicle) {
        val entity = vehicle.toEntity()
        dao.saveAndMaybeActivate(entity, activateIfNone = true)
    }

    override fun findByRego(rego: String): Vehicle? =
        dao.findByRego(Vehicle.normalizeRego(rego))?.toDomain()

    override fun all(): List<Vehicle> = dao.all().map { it.toDomain() }

    override fun setActive(rego: String): Boolean {
        val key = Vehicle.normalizeRego(rego)
        if (dao.findByRego(key) == null) return false
        dao.setActiveRow(ActiveVehicleEntity(rego = key))
        return true
    }
}
