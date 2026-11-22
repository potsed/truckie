package com.truckie1.app.profiles

import com.truckie1.profiles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleEntityMapperTest {
    @Test
    fun roundTripsExpandedFieldsAndNormalizesRego() {
        val domain = Vehicle(
            rego = "abc-123",
            nickname = "Blue rigid",
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
        val entity = domain.toEntity()
        assertEquals("ABC123", entity.rego)
        assertEquals(26.0, entity.gvmT, 0.0)
        assertEquals(8.5, entity.tareT, 0.0)
        assertEquals(42.5, entity.gcmT, 0.0)
        assertEquals("Volvo", entity.make)

        val back = entity.toDomain()
        assertEquals("ABC123", back.rego)
        assertEquals(domain.nickname, back.nickname)
        assertEquals(domain.year, back.year)
        assertEquals(domain.gcmT, back.gcmT, 0.0)
    }
}
