package com.truckie1.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapLibreNativeEngineTest {
    @Test
    fun reportsMapLibreIdentity() {
        val engine = MapLibreNativeEngine()
        assertEquals("MapLibre", engine.engineName)
        assertFalse(engine.isOfflinePackManagerAvailable())
        assertEquals(emptyList<String>(), engine.listOfflinePackIds())
    }
}
