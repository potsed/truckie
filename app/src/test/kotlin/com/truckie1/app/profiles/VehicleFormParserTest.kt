package com.truckie1.app.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleFormParserTest {
    private val valid = VehicleFormParser.Input(
        rego = "ABC-123",
        nickname = "Blue rigid",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = "2019",
        heightM = "3.0",
        widthM = "2.5",
        lengthM = "12.5",
        massT = "22.5",
        tareT = "8.5",
        gvmT = "26.0",
        gcmT = "42.5",
        axles = "3",
        networkClass = "GML",
    )

    @Test
    fun parsesFullProfileWithoutLoadHeightOrBuffer() {
        val result = VehicleFormParser.parse(valid)
        assertTrue(result is VehicleFormParser.Result.Ok)
        val vehicle = (result as VehicleFormParser.Result.Ok).vehicle
        assertEquals("ABC-123", vehicle.rego)
        assertEquals("Volvo", vehicle.make)
        assertEquals("FM", vehicle.model)
        assertEquals(2019, vehicle.year)
        assertEquals(8.5, vehicle.tareT, 0.0)
        assertEquals(26.0, vehicle.gvmT, 0.0)
        assertEquals(42.5, vehicle.gcmT, 0.0)
        assertEquals("GML", vehicle.networkClass)
    }

    @Test
    fun rejectsMissingMake() {
        val result = VehicleFormParser.parse(valid.copy(make = " "))
        assertTrue(result is VehicleFormParser.Result.Err)
    }

    @Test
    fun rejectsNonNumericGvm() {
        val result = VehicleFormParser.parse(valid.copy(gvmT = "x"))
        assertTrue(result is VehicleFormParser.Result.Err)
    }
}
