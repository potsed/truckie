package com.truckie1.app.profiles

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVehicleSummaryTest {
    private fun vehicle(rego: String = "ABC123") = Vehicle(
        rego = rego,
        nickname = "Blue",
        vehicleType = "Rigid",
        make = "Volvo",
        model = "FM",
        year = 2019,
        heightM = 3.0,
        widthM = 2.5,
        lengthM = 12.5,
        massT = 22.5,
        tareT = 8.5,
        gvmT = 26.0,
        gcmT = 42.5,
        axleCount = 3,
        networkClass = "GML",
    )

    @Test
    fun returnsNullWhenFlagOff() {
        val flags = LocalFeatureFlags()
        val repo = InMemoryVehicleProfileRepository()
        repo.save(vehicle())
        assertNull(HomeVehicleSummary.text(flags, repo))
    }

    @Test
    fun showsActiveVehicleWhenFlagOn() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.VEHICLE_PROFILES to true))
        val repo = InMemoryVehicleProfileRepository()
        repo.save(vehicle())
        val text = HomeVehicleSummary.text(flags, repo)
        assertTrue(text!!.contains("ABC123"))
        assertTrue(text.contains("Rigid"))
        assertTrue(text.contains("3.0"))
    }

    @Test
    fun showsPlaceholderWhenFlagOnButNoVehicle() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.VEHICLE_PROFILES to true))
        val repo = InMemoryVehicleProfileRepository()
        assertEquals("No active vehicle", HomeVehicleSummary.text(flags, repo))
    }
}
