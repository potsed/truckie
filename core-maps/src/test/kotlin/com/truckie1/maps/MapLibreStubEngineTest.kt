package com.truckie1.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLibreStubEngineTest {
    @Test
    fun stubReportsMapLibreIdentity() {
        val engine = MapLibreStubEngine()
        assertEquals("MapLibre-stub", engine.engineName)
        assertTrue(engine.isOfflinePackManagerAvailable())
        engine.registerPackForTests("qld-se")
        assertEquals(listOf("qld-se"), engine.listOfflinePackIds())
    }
}
