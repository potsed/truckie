package com.truckie1.app.trips

import com.truckie1.trips.CargoAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopFormParserTest {
    @Test
    fun requiresAddress() {
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = "  ",
                cargoEnabled = false,
                cargoItems = emptyList(),
            ),
        )
        assertTrue(result is StopFormParser.Result.Err)
    }

    @Test
    fun parsesAddressOnlyWhenCargoFlagOff() {
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = "Depot",
                cargoEnabled = false,
                cargoItems = listOf(
                    StopFormParser.CargoInput(
                        action = "PICK_UP",
                        description = "crates",
                        heightM = "3.5",
                        widthM = "2.4",
                        lengthM = "6.0",
                        massT = "8.0",
                    ),
                ),
            ),
        )
        val ok = result as StopFormParser.Result.Ok
        assertEquals("Depot", ok.stop.addressLabel)
        assertTrue(ok.stop.cargo.isEmpty())
    }

    @Test
    fun parsesPickUpCargoWhenFlagOn() {
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = "Depot",
                cargoEnabled = true,
                cargoItems = listOf(
                    StopFormParser.CargoInput(
                        action = "PICK_UP",
                        description = "Steel crates",
                        heightM = "3.5",
                        widthM = "2.4",
                        lengthM = "6.0",
                        massT = "8.0",
                    ),
                ),
            ),
        )
        val ok = result as StopFormParser.Result.Ok
        assertEquals(1, ok.stop.cargo.size)
        val cargo = ok.stop.cargo.single()
        assertEquals(CargoAction.PICK_UP, cargo.action)
        assertEquals("Steel crates", cargo.description)
        assertEquals(3.5, cargo.heightM, 1e-9)
        assertEquals(8.0, cargo.massT, 1e-9)
    }

    @Test
    fun rejectsIncompleteCargoRow() {
        val result = StopFormParser.parse(
            StopFormParser.Input(
                addressLabel = "Depot",
                cargoEnabled = true,
                cargoItems = listOf(
                    StopFormParser.CargoInput(
                        action = "DROP_OFF",
                        description = "crates",
                        heightM = "",
                        widthM = "2.4",
                        lengthM = "6.0",
                        massT = "8.0",
                    ),
                ),
            ),
        )
        assertTrue(result is StopFormParser.Result.Err)
    }
}
