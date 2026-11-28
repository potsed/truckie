package com.truckie1.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/feature_flag_persist.feature — seedIfAbsent + persist hook.
 */
class LocalFeatureFlagsPersistHookTest {

    @Test
    fun seedIfAbsentDoesNotOverwriteExistingOverride() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.MAP_HEADING_UP to false))
        flags.seedIfAbsent(FlagKeys.MAP_HEADING_UP, enabled = true)
        assertFalse(flags.isEnabled(FlagKeys.MAP_HEADING_UP))
        assertTrue(flags.hasOverride(FlagKeys.MAP_HEADING_UP))
    }

    @Test
    fun seedIfAbsentFillsMissingKeyWithoutNotifyingListener() {
        val changes = mutableListOf<Pair<String, Boolean>>()
        val flags = LocalFeatureFlags(
            onOverrideChanged = { key, enabled -> changes += key to enabled },
        )
        flags.seedIfAbsent(FlagKeys.ACTIVE_TOUR, enabled = true)
        assertTrue(flags.isEnabled(FlagKeys.ACTIVE_TOUR))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun setEnabledNotifiesPersistListener() {
        val changes = mutableListOf<Pair<String, Boolean>>()
        val flags = LocalFeatureFlags(
            onOverrideChanged = { key, enabled -> changes += key to enabled },
        )
        flags.setEnabled(FlagKeys.DEMO_MODE, true)
        assertEquals(listOf(FlagKeys.DEMO_MODE to true), changes)
        flags.setEnabled(FlagKeys.DEMO_MODE, false)
        assertEquals(
            listOf(FlagKeys.DEMO_MODE to true, FlagKeys.DEMO_MODE to false),
            changes,
        )
    }

    @Test
    fun initialMapCountsAsOverridesWithoutNotifying() {
        val changes = mutableListOf<Pair<String, Boolean>>()
        val flags = LocalFeatureFlags(
            initial = mapOf(FlagKeys.DEMO_MODE to true),
            onOverrideChanged = { key, enabled -> changes += key to enabled },
        )
        assertTrue(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertTrue(flags.hasOverride(FlagKeys.DEMO_MODE))
        assertTrue(changes.isEmpty())
    }
}
