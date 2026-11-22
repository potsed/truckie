package com.truckie1.app.profiles

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.truckie1.profiles.Vehicle

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val rego: String,
    val nickname: String,
    val vehicleType: String,
    val make: String,
    val model: String,
    val year: Int,
    val heightM: Double,
    val widthM: Double,
    val lengthM: Double,
    val massT: Double,
    val tareT: Double,
    val gvmT: Double,
    val gcmT: Double,
    val axleCount: Int,
    val networkClass: String?,
    val notes: String?,
)

@Entity(tableName = "active_vehicle")
data class ActiveVehicleEntity(
    @PrimaryKey val id: Int = 1,
    val rego: String,
)

fun VehicleEntity.toDomain(): Vehicle =
    Vehicle(
        rego = rego,
        nickname = nickname,
        vehicleType = vehicleType,
        make = make,
        model = model,
        year = year,
        heightM = heightM,
        widthM = widthM,
        lengthM = lengthM,
        massT = massT,
        tareT = tareT,
        gvmT = gvmT,
        gcmT = gcmT,
        axleCount = axleCount,
        networkClass = networkClass,
        notes = notes,
    )

fun Vehicle.toEntity(): VehicleEntity =
    VehicleEntity(
        rego = Vehicle.normalizeRego(rego),
        nickname = nickname,
        vehicleType = vehicleType,
        make = make,
        model = model,
        year = year,
        heightM = heightM,
        widthM = widthM,
        lengthM = lengthM,
        massT = massT,
        tareT = tareT,
        gvmT = gvmT,
        gcmT = gcmT,
        axleCount = axleCount,
        networkClass = networkClass,
        notes = notes,
    )
