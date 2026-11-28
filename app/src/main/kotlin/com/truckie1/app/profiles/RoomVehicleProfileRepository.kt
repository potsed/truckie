package com.truckie1.app.profiles

import com.truckie1.profiles.Vehicle
import com.truckie1.profiles.VehicleProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Room-backed vehicle profiles keyed by normalized rego.
 * DAO calls hop off the caller thread via [io] (no allowMainThreadQueries).
 */
class RoomVehicleProfileRepository(
    private val dao: VehicleDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : VehicleProfileRepository {
    private fun <T> db(block: suspend () -> T): T = runBlocking(io) { block() }

    override var activeRego: String?
        get() = db { dao.activeRego() }
        set(value) {
            db {
                if (value == null) {
                    dao.clearActive()
                } else {
                    val key = Vehicle.normalizeRego(value)
                    if (dao.findByRego(key) != null) {
                        dao.setActiveRow(ActiveVehicleEntity(rego = key))
                    }
                }
            }
        }

    override fun save(vehicle: Vehicle) {
        val entity = vehicle.toEntity()
        db { dao.saveAndMaybeActivate(entity, activateIfNone = true) }
    }

    override fun findByRego(rego: String): Vehicle? =
        db { dao.findByRego(Vehicle.normalizeRego(rego))?.toDomain() }

    override fun all(): List<Vehicle> = db { dao.all().map { it.toDomain() } }

    override fun setActive(rego: String): Boolean {
        val key = Vehicle.normalizeRego(rego)
        return db {
            if (dao.findByRego(key) == null) {
                false
            } else {
                dao.setActiveRow(ActiveVehicleEntity(rego = key))
                true
            }
        }
    }
}
