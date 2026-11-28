package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/async_routing_io.feature — async routing I/O catalog.
 */
class AsyncRoutingIoCatalogTest {

    @Test
    fun flagKeyAndDefaultsOnConstant() {
        assertEquals("ff_async_routing_io", FlagKeys.ASYNC_ROUTING_IO)
        assertEquals(FlagKeys.ASYNC_ROUTING_IO, AsyncRoutingIoCatalog.FLAG_KEY)
        assertTrue(AsyncRoutingIoCatalog.DEFAULT_ENABLED)
        assertTrue(AsyncRoutingIoCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertFalse(AsyncRoutingIoCatalog.enablesGraphHopperLiveByDefault)
        assertTrue(AsyncRoutingIoCatalog.livePathRouterIsSuspend)
        assertTrue(AsyncRoutingIoCatalog.nominatimSuggestIsSuspend)
        assertTrue(AsyncRoutingIoCatalog.neverBlocksComposeRemember)
        assertTrue(AsyncRoutingIoCatalog.neverBlocksMainThreadClickForLiveHttp)
        assertTrue(AsyncRoutingIoCatalog.stubPathRouterMayStayInstantaneous)
        assertTrue(AsyncRoutingIoCatalog.reusesMapRoutePolylineSessionCache)
        assertTrue(AsyncRoutingIoCatalog.SUGGEST_DEBOUNCE_MS > 0L)
    }

    @Test
    fun setEnabledTogglesFlag() {
        val flags = LocalFeatureFlags()
        assertFalse(AsyncRoutingIoCatalog.isEnabled(flags))
        AsyncRoutingIoCatalog.setEnabled(flags, true)
        assertTrue(AsyncRoutingIoCatalog.isEnabled(flags))
        AsyncRoutingIoCatalog.setEnabled(flags, false)
        assertFalse(AsyncRoutingIoCatalog.isEnabled(flags))
    }

    @Test
    fun liveGraphHopperRequiresAsyncAndLiveFlags() {
        val flags = LocalFeatureFlags()
        assertFalse(AsyncRoutingIoCatalog.allowsLiveGraphHopper(flags))
        flags.setEnabled(FlagKeys.GRAPHHOPPER_LIVE, true)
        assertFalse(AsyncRoutingIoCatalog.allowsLiveGraphHopper(flags))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        assertTrue(AsyncRoutingIoCatalog.allowsLiveGraphHopper(flags))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, false)
        assertFalse(AsyncRoutingIoCatalog.allowsLiveGraphHopper(flags))
    }

    @Test
    fun liveNominatimRequiresAsyncAndAutocompleteFlags() {
        val flags = LocalFeatureFlags()
        assertFalse(AsyncRoutingIoCatalog.allowsLiveNominatim(flags))
        flags.setEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE, true)
        assertFalse(AsyncRoutingIoCatalog.allowsLiveNominatim(flags))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        assertTrue(AsyncRoutingIoCatalog.allowsLiveNominatim(flags))
    }

    @Test
    fun liveValhallaRequiresAsyncAndLiveFlags() {
        val flags = LocalFeatureFlags()
        assertFalse(AsyncRoutingIoCatalog.allowsLiveValhalla(flags))
        flags.setEnabled(FlagKeys.VALHALLA_LIVE, true)
        assertFalse(AsyncRoutingIoCatalog.allowsLiveValhalla(flags))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        assertTrue(AsyncRoutingIoCatalog.allowsLiveValhalla(flags))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, false)
        assertFalse(AsyncRoutingIoCatalog.allowsLiveValhalla(flags))
    }
}
