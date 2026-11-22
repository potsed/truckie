package com.truckie1.app.profiles

import com.truckie1.profiles.Vehicle

/**
 * Parses edit-screen fields into a [Vehicle]. Pure for unit tests.
 * Does not include load height or clearance buffer (those live on trip/settings).
 */
object VehicleFormParser {
    data class Input(
        val rego: String,
        val nickname: String,
        val vehicleType: String,
        val make: String,
        val model: String,
        val year: String,
        val heightM: String,
        val widthM: String,
        val lengthM: String,
        val massT: String,
        val tareT: String,
        val gvmT: String,
        val gcmT: String,
        val axles: String,
        val networkClass: String,
    )

    sealed class Result {
        data class Ok(val vehicle: Vehicle) : Result()
        data class Err(val message: String) : Result()
    }

    fun parse(input: Input): Result {
        val rego = input.rego.trim()
        if (rego.isEmpty()) return Result.Err("Rego is required")
        val nickname = input.nickname.trim().ifEmpty { rego }
        val vehicleType = input.vehicleType.trim().ifEmpty { "Rigid" }
        val make = input.make.trim()
        val model = input.model.trim()
        if (make.isEmpty()) return Result.Err("Make is required")
        if (model.isEmpty()) return Result.Err("Model is required")

        val year = input.year.toIntOrNull()
            ?: return Result.Err("Year must be a number")
        val heightM = input.heightM.toDoubleOrNull()
            ?: return Result.Err("Height must be a number")
        val widthM = input.widthM.toDoubleOrNull()
            ?: return Result.Err("Width must be a number")
        val lengthM = input.lengthM.toDoubleOrNull()
            ?: return Result.Err("Length must be a number")
        val massT = input.massT.toDoubleOrNull()
            ?: return Result.Err("Mass must be a number")
        val tareT = input.tareT.toDoubleOrNull()
            ?: return Result.Err("TARE must be a number")
        val gvmT = input.gvmT.toDoubleOrNull()
            ?: return Result.Err("GVM must be a number")
        val gcmT = input.gcmT.toDoubleOrNull()
            ?: return Result.Err("GCM must be a number")
        val axles = input.axles.toIntOrNull()
            ?: return Result.Err("Axles must be a number")
        val network = input.networkClass.trim().ifEmpty { null }

        return try {
            Result.Ok(
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
                    axleCount = axles,
                    networkClass = network,
                ),
            )
        } catch (e: IllegalArgumentException) {
            Result.Err(e.message ?: "Invalid vehicle")
        }
    }
}
