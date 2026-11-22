package com.truckie1.app.trips

import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.TripStop
import java.util.UUID

/**
 * Parses add-stop form fields into a [TripStop]. Pure for unit tests.
 * When [Input.cargoEnabled] is false, cargo rows are ignored.
 */
object StopFormParser {
    data class CargoInput(
        val action: String,
        val description: String,
        val heightM: String,
        val widthM: String,
        val lengthM: String,
        val massT: String,
    )

    data class Input(
        val addressLabel: String,
        val cargoEnabled: Boolean,
        val cargoItems: List<CargoInput>,
        val stopId: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
    )

    sealed class Result {
        data class Ok(val stop: TripStop) : Result()
        data class Err(val message: String) : Result()
    }

    fun parse(input: Input): Result {
        val address = input.addressLabel.trim()
        if (address.isEmpty()) return Result.Err("Address is required")

        val cargoParsed = if (!input.cargoEnabled) {
            emptyList()
        } else {
            val items = mutableListOf<Cargo>()
            for ((index, row) in input.cargoItems.withIndex()) {
                if (isBlankCargoRow(row)) continue
                when (val item = parseCargoItem(row, index + 1)) {
                    is CargoParse.Err -> return Result.Err(item.message)
                    is CargoParse.Ok -> items += item.cargo
                }
            }
            items
        }

        return Result.Ok(
            TripStop(
                id = input.stopId ?: UUID.randomUUID().toString(),
                addressLabel = address,
                latitude = input.latitude,
                longitude = input.longitude,
                cargo = cargoParsed,
            ),
        )
    }

    private fun isBlankCargoRow(row: CargoInput): Boolean =
        row.description.isBlank() &&
            row.heightM.isBlank() &&
            row.widthM.isBlank() &&
            row.lengthM.isBlank() &&
            row.massT.isBlank()

    private sealed class CargoParse {
        data class Ok(val cargo: Cargo) : CargoParse()
        data class Err(val message: String) : CargoParse()
    }

    private fun parseCargoItem(row: CargoInput, rowNumber: Int): CargoParse {
        val description = row.description.trim()
        if (description.isEmpty()) {
            return CargoParse.Err("Cargo #$rowNumber description is required")
        }
        val action = when (row.action.trim().uppercase()) {
            "PICK_UP", "PICKUP", "PICK UP" -> CargoAction.PICK_UP
            "DROP_OFF", "DROPOFF", "DROP OFF" -> CargoAction.DROP_OFF
            else -> return CargoParse.Err("Cargo #$rowNumber action must be pick up or drop off")
        }
        val heightM = row.heightM.toDoubleOrNull()
            ?: return CargoParse.Err("Cargo #$rowNumber height must be a number")
        val widthM = row.widthM.toDoubleOrNull()
            ?: return CargoParse.Err("Cargo #$rowNumber width must be a number")
        val lengthM = row.lengthM.toDoubleOrNull()
            ?: return CargoParse.Err("Cargo #$rowNumber length must be a number")
        val massT = row.massT.toDoubleOrNull()
            ?: return CargoParse.Err("Cargo #$rowNumber mass must be a number")
        return try {
            CargoParse.Ok(
                Cargo(
                    description = description,
                    heightM = heightM,
                    widthM = widthM,
                    lengthM = lengthM,
                    massT = massT,
                    action = action,
                ),
            )
        } catch (e: IllegalArgumentException) {
            CargoParse.Err(e.message ?: "Invalid cargo #$rowNumber")
        }
    }
}
