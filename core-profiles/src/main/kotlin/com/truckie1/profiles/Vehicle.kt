package com.truckie1.profiles

/**
 * Vehicle profile keyed by Australian registration (rego).
 * UI and public APIs use "Vehicle", never "Truck".
 */
data class Vehicle(
    val rego: String,
    val displayName: String,
    val heightM: Double,
    val widthM: Double,
    val lengthM: Double,
    val grossMassT: Double,
    val axleCount: Int = 2,
    val networkClass: String? = null,
    val notes: String? = null,
) {
    init {
        require(rego.isNotBlank()) { "rego must not be blank" }
        require(heightM > 0) { "heightM must be positive" }
        require(widthM > 0) { "widthM must be positive" }
        require(lengthM > 0) { "lengthM must be positive" }
        require(grossMassT > 0) { "grossMassT must be positive" }
    }

    companion object {
        fun normalizeRego(raw: String): String =
            raw.trim().uppercase().replace("\\s+".toRegex(), "")
    }
}

interface VehicleProfileRepository {
    fun save(vehicle: Vehicle)
    fun findByRego(rego: String): Vehicle?
    fun all(): List<Vehicle>
    var activeRego: String?
}

class InMemoryVehicleProfileRepository : VehicleProfileRepository {
    private val byRego = linkedMapOf<String, Vehicle>()
    override var activeRego: String? = null

    override fun save(vehicle: Vehicle) {
        val key = Vehicle.normalizeRego(vehicle.rego)
        byRego[key] = vehicle.copy(rego = key)
        if (activeRego == null) activeRego = key
    }

    override fun findByRego(rego: String): Vehicle? = byRego[Vehicle.normalizeRego(rego)]

    override fun all(): List<Vehicle> = byRego.values.toList()
}
