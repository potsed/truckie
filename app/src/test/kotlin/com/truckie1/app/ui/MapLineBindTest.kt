package com.truckie1.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shared LineManager obtain path for Navigate + home route polyline.
 */
class MapLineBindTest {

    @Test
    fun sharesPathWithNavigatePolyline() {
        assertTrue(MapLineBind.sharesPathWithNavigatePolyline)
        assertTrue(MapLineBind.usesMapLibreStyleLifecycle)
        assertTrue(MapRoutePathAnnotations.usesLineManager)
        assertTrue(MapRoutePolylineCatalog.usesSharedLineBindPath)
    }
}
