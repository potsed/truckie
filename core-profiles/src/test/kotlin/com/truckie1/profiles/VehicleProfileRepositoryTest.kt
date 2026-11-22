package com.truckie1.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VehicleProfileRepositoryTest {
    @Test
    fun savesAndActivatesByNormalizedRego() {
        val repo = InMemoryVehicleProfileRepository()
        repo.save(
            Vehicle(
                rego = "abc 123",
                displayName = "Blue rigid",
                heightM = 3.0,
                widthM = 2.5,
                lengthM = 12.5,
                grossMassT = 22.5,
            ),
        )
        assertEquals("ABC123", repo.activeRego)
        assertNotNull(repo.findByRego("ABC-123".replace("-", "")))
        assertEquals(3.0, repo.findByRego("ABC123")!!.heightM, 0.0)
    }
}
