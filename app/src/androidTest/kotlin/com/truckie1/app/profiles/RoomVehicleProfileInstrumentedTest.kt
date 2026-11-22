package com.truckie1.app.profiles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truckie1.profiles.Vehicle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomVehicleProfileInstrumentedTest {
    private lateinit var db: VehicleDatabase
    private lateinit var repo: RoomVehicleProfileRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = VehicleDatabase.inMemory(context)
        repo = RoomVehicleProfileRepository(db.vehicleDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun savesExpandedMassRatings() {
        repo.save(
            Vehicle(
                rego = "QLD441",
                nickname = "Tipper",
                vehicleType = "Tipper",
                make = "Isuzu",
                model = "FXZ",
                year = 2021,
                heightM = 3.2,
                widthM = 2.5,
                lengthM = 10.0,
                massT = 18.0,
                tareT = 7.2,
                gvmT = 24.0,
                gcmT = 40.0,
                axleCount = 3,
                networkClass = "HML",
            ),
        )
        val found = repo.findByRego("QLD441")!!
        assertEquals(7.2, found.tareT, 0.0)
        assertEquals(24.0, found.gvmT, 0.0)
        assertEquals(40.0, found.gcmT, 0.0)
        assertEquals("Isuzu", found.make)
    }
}
