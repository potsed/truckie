package com.truckie1.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirrors features/vehicle_profile_by_rego.feature (ff_vehicle_profiles).
 */
class VehicleProfileRepositoryTest {
    private fun sample(
        rego: String = "ABC-123",
        nickname: String = "Blue rigid",
    ) = Vehicle(
        rego = rego,
        nickname = nickname,
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
    fun savesAndActivatesByNormalizedRego() {
        val repo = InMemoryVehicleProfileRepository()
        repo.save(sample(rego = "abc 123"))

        assertEquals("ABC123", repo.activeRego)
        val found = repo.findByRego("ABC123")
        assertNotNull(found)
        assertEquals(3.0, found!!.heightM, 0.0)
        assertEquals(26.0, found.gvmT, 0.0)
        assertEquals("Volvo", found.make)
        assertEquals("FM", found.model)
        assertEquals(2019, found.year)
        assertEquals(8.5, found.tareT, 0.0)
        assertEquals(42.5, found.gcmT, 0.0)
        assertEquals("Rigid", found.vehicleType)
        assertEquals(3, found.axleCount)
        assertEquals("GML", found.networkClass)
        assertEquals("Blue rigid", found.nickname)
    }

    @Test
    fun selectActiveVehicleFromList() {
        val repo = InMemoryVehicleProfileRepository()
        repo.save(sample(rego = "ABC123"))
        repo.save(sample(rego = "XYZ987", nickname = "B-double"))

        repo.setActive("XYZ987")

        assertEquals("XYZ987", repo.activeRego)
        assertEquals(2, repo.all().size)
    }

    @Test
    fun setActiveRequiresKnownRego() {
        val repo = InMemoryVehicleProfileRepository()
        repo.save(sample())
        assertFalse(repo.setActive("UNKNOWN"))
        assertEquals("ABC123", repo.activeRego)
        assertTrue(repo.setActive("ABC123"))
    }

    @Test
    fun findByRegoNormalizesHyphensAndSpaces() {
        val repo = InMemoryVehicleProfileRepository()
        repo.save(sample(rego = "ABC-123"))
        assertNotNull(repo.findByRego("abc 123"))
        assertNotNull(repo.findByRego("ABC-123"))
        assertNull(repo.findByRego("NOPE"))
    }
}
