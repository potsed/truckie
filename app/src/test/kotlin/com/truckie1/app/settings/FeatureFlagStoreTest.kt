package com.truckie1.app.settings

import com.truckie1.app.ui.DemoModeCatalog
import com.truckie1.app.ui.DemoModeController
import com.truckie1.app.ui.MapHeadingUpCatalog
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/feature_flag_persist.feature, features/demo_mode.feature —
 * durable FeatureFlagStore path (ATNA-93).
 */
class FeatureFlagStoreTest {

    @Test
    fun freshStoreKeepsDemoAndUserFlagsOff() {
        val store = InMemoryFeatureFlagStore()
        val flags = FeatureFlagPersistence.bind(store)
        assertFalse(DemoModeCatalog.isEnabled(flags))
        assertFalse(flags.isEnabled(FlagKeys.MAP_HOME))
        assertFalse(flags.isEnabled(FlagKeys.VEHICLE_PROFILES))
        assertTrue(store.loadAll().isEmpty())
    }

    @Test
    fun demoModeToggleSurvivesNewFlagsSession() {
        val store = InMemoryFeatureFlagStore()
        val flags = FeatureFlagPersistence.bind(store)
        DemoModeController(flags).setEnabled(true)
        assertTrue(DemoModeCatalog.isEnabled(flags))
        assertEquals(true, store.loadAll()[FlagKeys.DEMO_MODE])

        val restarted = FeatureFlagPersistence.bind(store)
        assertTrue(DemoModeCatalog.isEnabled(restarted))
    }

    @Test
    fun demoModeOffPersistsAcrossRestart() {
        val store = InMemoryFeatureFlagStore(
            initial = mapOf(FlagKeys.DEMO_MODE to true),
        )
        val flags = FeatureFlagPersistence.bind(store)
        assertTrue(DemoModeCatalog.isEnabled(flags))
        DemoModeController(flags).setEnabled(false)
        assertEquals(false, store.loadAll()[FlagKeys.DEMO_MODE])

        val restarted = FeatureFlagPersistence.bind(store)
        assertFalse(DemoModeCatalog.isEnabled(restarted))
    }

    @Test
    fun headingUpSurvivesNewFlagsSession() {
        val store = InMemoryFeatureFlagStore()
        val flags = FeatureFlagPersistence.bind(store)
        MapHeadingUpCatalog.setEnabled(flags, enabled = true)
        assertTrue(MapHeadingUpCatalog.isEnabled(flags))

        val restarted = FeatureFlagPersistence.bind(store)
        assertTrue(MapHeadingUpCatalog.isEnabled(restarted))
    }

    @Test
    fun debugSeedIfAbsentDoesNotOverwritePersistedOff() {
        val store = InMemoryFeatureFlagStore(
            initial = mapOf(FlagKeys.MAP_HEADING_UP to false),
        )
        val flags = FeatureFlagPersistence.bind(store)
        flags.seedIfAbsent(FlagKeys.MAP_HEADING_UP, enabled = true)
        assertFalse(flags.isEnabled(FlagKeys.MAP_HEADING_UP))
        assertEquals(false, store.loadAll()[FlagKeys.MAP_HEADING_UP])
    }

    @Test
    fun structuralSeedDoesNotWriteStore() {
        val store = InMemoryFeatureFlagStore()
        val flags = FeatureFlagPersistence.bind(store)
        flags.seedIfAbsent(FlagKeys.ACTIVE_TOUR, enabled = true)
        assertTrue(flags.isEnabled(FlagKeys.ACTIVE_TOUR))
        assertFalse(store.loadAll().containsKey(FlagKeys.ACTIVE_TOUR))
    }

    @Test
    fun demoUsesSameStoreAsOtherFlagOverrides() {
        val store = InMemoryFeatureFlagStore()
        val flags = FeatureFlagPersistence.bind(store)
        DemoModeController(flags).setEnabled(true)
        MapHeadingUpCatalog.setEnabled(flags, enabled = true)
        val snapshot = store.loadAll()
        assertEquals(true, snapshot[FlagKeys.DEMO_MODE])
        assertEquals(true, snapshot[FlagKeys.MAP_HEADING_UP])
        assertEquals(2, snapshot.size)
    }

    @Test
    fun dataStoreNameIsDedicatedFeatureFlagsFile() {
        assertEquals("truckie_feature_flags", FeatureFlagPersistence.DATA_STORE_NAME)
        assertEquals("feature_flags", FeatureFlagPersistence.CATALOG_ID)
    }
}
