package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/path_block_detour.feature — ff_path_block_detour catalog.
 */
class PathBlockDetourCatalogTest {

    @Test
    fun flagDefaultsOnAndUsesPathRouter() {
        assertEquals("ff_path_block_detour", FlagKeys.PATH_BLOCK_DETOUR)
        assertEquals(FlagKeys.PATH_BLOCK_DETOUR, PathBlockDetourCatalog.FLAG_KEY)
        assertTrue(PathBlockDetourCatalog.DEFAULT_ENABLED)
        assertTrue(PathBlockDetourCatalog.usesPathRouter)
        assertFalse(PathBlockDetourCatalog.usesHandRolledPolylineRule)
        assertTrue(PathBlockDetourCatalog.prefersLiveValhalla)
        val flags = LocalFeatureFlags()
        assertFalse(PathBlockDetourCatalog.isEnabled(flags))
        PathBlockDetourCatalog.setEnabled(flags, true)
        assertTrue(PathBlockDetourCatalog.isEnabled(flags))
    }
}
