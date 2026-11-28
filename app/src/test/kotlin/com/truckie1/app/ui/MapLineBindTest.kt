package com.truckie1.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shared style-layer route line path for Navigate + home route polyline.
 */
class MapLineBindTest {

    @Test
    fun sharesPathWithNavigatePolyline() {
        assertTrue(MapLineBind.sharesPathWithNavigatePolyline)
        assertTrue(MapLineBind.usesMapLibreStyleLifecycle)
        assertTrue(MapRoutePathStyle.usesStyleLineLayer)
        assertFalse(MapRoutePathStyle.usesLineManager)
        assertTrue(MapRoutePolylineCatalog.usesSharedStyleOverlayBindPath)
        assertFalse(MapStyleOverlayCatalog.usesAnnotationLineManager)
    }
}
