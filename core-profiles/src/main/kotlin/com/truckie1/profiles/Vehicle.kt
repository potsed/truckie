package com.truckie1.profiles

/**
 * Vehicle profile keyed by Australian registration (rego).
 * UI and public APIs use "Vehicle", never "Truck".
 *
 * Clearance buffer and load/cargo height live on trip/settings — not on this profile.
 */
data class Vehicle(
    val rego: String,
    val nickname: String,
    val vehicleType: String,
    val make: String,
    val model: String,
    val year: Int,
    val heightM: Double,
    val widthM: Double,
    val lengthM: Double,
    /** Current / planned operating mass (tonnes). */
    val massT: Double,
    /** Unladen mass (tonnes). */
    val tareT: Double,
    /** Gross Vehicle Mass rating (tonnes). */
    val gvmT: Double,
    /** Gross Combination Mass rating (tonnes). */
    val gcmT: Double,
    val axleCount: Int = 2,
    val networkClass: String? = null,
    val notes: String? = null,
) {
    /** Alias used by older call sites / Home labels. */
    val displayName: String get() = nickname

    /** Alias for height-focused Home summary. */
    val grossMassT: Double get() = massT

    init {
        require(rego.isNotBlank()) { "rego must not be blank" }
        require(year in 1900..2100) { "year out of range" }
        require(heightM > 0) { "heightM must be positive" }
        require(widthM > 0) { "widthM must be positive" }
        require(lengthM > 0) { "lengthM must be positive" }
        require(massT > 0) { "massT must be positive" }
        require(tareT > 0) { "tareT must be positive" }
        require(gvmT > 0) { "gvmT must be positive" }
        require(gcmT > 0) { "gcmT must be positive" }
        require(axleCount > 0) { "axleCount must be positive" }
    }

    companion object {
        fun normalizeRego(raw: String): String =
            raw.trim().uppercase().replace("[\\s\\-]+".toRegex(), "")
    }
}

interface VehicleProfileRepository {
    fun save(vehicle: Vehicle)
    fun findByRego(rego: String): Vehicle?
    fun all(): List<Vehicle>
    var activeRego: String?

    /** Sets active vehicle; returns false if rego is unknown. */
    fun setActive(rego: String): Boolean
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

    override fun setActive(rego: String): Boolean {
        val key = Vehicle.normalizeRego(rego)
        if (!byRego.containsKey(key)) return false
        activeRego = key
        return true
    }
}
