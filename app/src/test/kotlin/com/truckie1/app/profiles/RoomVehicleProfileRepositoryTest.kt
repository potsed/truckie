package com.truckie1.app.profiles

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.truckie1.profiles.Vehicle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RoomVehicleProfileRepositoryTest {
    private lateinit var db: VehicleDatabase
    private lateinit var repo: RoomVehicleProfileRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = VehicleDatabase.inMemory(context)
        repo = RoomVehicleProfileRepository(db.vehicleDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sample(rego: String) = Vehicle(
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
    fun persistsByRegoAndActivatesFirst() {
        repo.save(sample("ABC-123"))
        assertEquals("ABC123", repo.activeRego)
        val found = repo.findByRego("abc 123")
        assertNotNull(found)
        assertEquals(26.0, found!!.gvmT, 0.0)
        assertEquals("Volvo", found.make)
        assertEquals(2019, found.year)
    }

    @Test
    fun selectActiveVehicle() {
        repo.save(sample("ABC123"))
        repo.save(sample("XYZ987"))
        assertTrue(repo.setActive("XYZ987"))
        assertEquals("XYZ987", repo.activeRego)
        assertEquals(2, repo.all().size)
    }
}
