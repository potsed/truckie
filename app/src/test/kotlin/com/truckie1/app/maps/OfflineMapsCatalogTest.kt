package com.truckie1.app.maps

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapLibreNativeEngine
import com.truckie1.maps.MapLibreStubEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/offline_maps.feature — ff_offline_maps behaviour contracts (ATNA-95).
 */
class OfflineMapsCatalogTest {

    @Test
    fun flagKeyDefaultsOffIncludingDebug() {
        assertEquals("ff_offline_maps", FlagKeys.OFFLINE_MAPS)
        assertEquals(FlagKeys.OFFLINE_MAPS, OfflineMapsCatalog.FLAG_KEY)
        assertFalse(OfflineMapsCatalog.DEFAULT_ENABLED)
        assertFalse(OfflineMapsCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.OFFLINE_MAPS))
        assertFalse(OfflineMapsCatalog.isEnabled(flags))
    }

    @Test
    fun packManagerHiddenWhenFlagOff() {
        val flags = LocalFeatureFlags()
        val engine = MapLibreNativeEngine()
        assertFalse(OfflineMapsCatalog.shouldExposePackManager(flags, engine))
        assertEquals(emptyList<String>(), OfflineMapsCatalog.listedPackIds(flags, engine))
        assertFalse(engine.isOfflinePackManagerAvailable())
    }

    @Test
    fun enablingFlagExposesPackManagerContract() {
        val flags = LocalFeatureFlags()
        OfflineMapsCatalog.setEnabled(flags, true)
        assertTrue(OfflineMapsCatalog.isEnabled(flags))
        assertTrue(OfflineMapsCatalog.targetsMapLibreOfflineManager)
        assertTrue(OfflineMapsCatalog.requiresPackDownloadUxBeforeProduction)
        assertTrue(OfflineMapsCatalog.requiresOfflineCapableStyleBeforeProduction)
        // Native engine still reports unavailable until OfflineManager lands;
        // product surface stays hidden until the engine can serve packs.
        assertFalse(
            OfflineMapsCatalog.shouldExposePackManager(flags, MapLibreNativeEngine()),
        )
    }

    @Test
    fun stubRegistersPacksWithoutEnablingProductionFlag() {
        val stub = MapLibreStubEngine()
        stub.registerPackForTests("qld-se")
        assertTrue(stub.isOfflinePackManagerAvailable())
        assertEquals(listOf("qld-se"), stub.listOfflinePackIds())
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.OFFLINE_MAPS))
        assertFalse(OfflineMapsCatalog.shouldExposePackManager(flags, stub))
        assertEquals(emptyList<String>(), OfflineMapsCatalog.listedPackIds(flags, stub))
        OfflineMapsCatalog.setEnabled(flags, true)
        assertTrue(OfflineMapsCatalog.shouldExposePackManager(flags, stub))
        assertEquals(listOf("qld-se"), OfflineMapsCatalog.listedPackIds(flags, stub))
    }
}
