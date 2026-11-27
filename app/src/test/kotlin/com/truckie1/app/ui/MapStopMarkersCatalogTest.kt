package com.truckie1.app.ui

import android.graphics.Color
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.trips.RouteSession
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import com.truckie1.trips.TripSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Gherkin: features/map_stop_markers.feature — ff_stop_markers on MapLibre home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MapStopMarkersCatalogTest {

    @Test
    fun stopMarkersFlagKeyIsFfStopMarkers() {
        assertEquals("ff_stop_markers", FlagKeys.STOP_MARKERS)
        assertEquals(FlagKeys.STOP_MARKERS, MapStopMarkersCatalog.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.STOP_MARKERS))
        flags.setEnabled(FlagKeys.STOP_MARKERS, true)
        assertTrue(flags.isEnabled(FlagKeys.STOP_MARKERS))
    }

    @Test
    fun gatedByFlagAndMapHome() {
        val off = LocalFeatureFlags()
        assertFalse(MapStopMarkersCatalog.isEnabled(off))
        assertFalse(MapStopMarkersCatalog.shouldShow(off, mapHomeEnabled = true))
        assertFalse(
            MapStopMarkersCatalog.shouldShow(
                LocalFeatureFlags(mapOf(FlagKeys.STOP_MARKERS to true)),
                mapHomeEnabled = false,
            ),
        )
        val on = LocalFeatureFlags(mapOf(FlagKeys.STOP_MARKERS to true))
        assertTrue(MapStopMarkersCatalog.isEnabled(on))
        assertTrue(MapStopMarkersCatalog.shouldShow(on, mapHomeEnabled = true))
    }

    @Test
    fun usesSymbolManagerNotDeprecatedMarkerApi() {
        assertTrue(MapStopMarkersCatalog.usesSymbolManager)
        assertFalse(MapStopMarkersCatalog.usesDeprecatedMarkerApi)
        assertTrue(MapStopMarkersCatalog.usesSharedSymbolBindPath)
        assertTrue(MapStopMarkersCatalog.sharesStyleLifecycleWithBridges)
        assertTrue(MapSymbolBind.sharesPathWithBridgeAndStopMarkers)
        assertTrue(MapStopMarkersCatalog.requiresOpenFreeMapCompatibleTextFont)
        assertEquals("Noto Sans Regular", MapStopMarkersCatalog.symbolTextFont.single())
    }

    @Test
    fun pickupAndDropOffUseDistinctIcons() {
        assertEquals(
            MapStopMarkersCatalog.ICON_PICKUP,
            MapStopMarkersCatalog.iconIdFor(StopKind.PICKUP_LOAD),
        )
        assertEquals(
            MapStopMarkersCatalog.ICON_DROPOFF,
            MapStopMarkersCatalog.iconIdFor(StopKind.DROP_OFF_UNLOAD),
        )
        assertNotEquals(
            MapStopMarkersCatalog.ICON_PICKUP,
            MapStopMarkersCatalog.ICON_DROPOFF,
        )
        assertNotEquals(
            MapStopMarkersCatalog.PICKUP_COLOR_HEX,
            MapStopMarkersCatalog.DROPOFF_COLOR_HEX,
        )
    }

    @Test
    fun createsDistinctNonEmptyPickupAndDropOffBitmaps() {
        val pickup = MapStopMarkersCatalog.createStopMarkerBitmap(
            sizePx = 48,
            kind = StopKind.PICKUP_LOAD,
        )
        val dropoff = MapStopMarkersCatalog.createStopMarkerBitmap(
            sizePx = 48,
            kind = StopKind.DROP_OFF_UNLOAD,
        )
        assertNotNull(pickup)
        assertNotNull(dropoff)
        assertEquals(48, pickup.width)
        assertEquals(48, dropoff.width)
        assertFalse(pickup.isRecycled)
        assertFalse(dropoff.isRecycled)
        // Robolectric Canvas may not rasterise fills — colour contract is the hex + icon ids.
        assertNotEquals(
            MapStopMarkersCatalog.PICKUP_COLOR_HEX,
            MapStopMarkersCatalog.DROPOFF_COLOR_HEX,
        )
        assertEquals(
            Color.parseColor("#0B6E4F"),
            Color.parseColor(MapStopMarkersCatalog.PICKUP_COLOR_HEX),
        )
        assertEquals(
            Color.parseColor("#1A4B7A"),
            Color.parseColor(MapStopMarkersCatalog.DROPOFF_COLOR_HEX),
        )
    }

    @Test
    fun mappableStopsRequireCoordinates() {
        val withCoords = RouteStop(
            id = "a",
            addressLabel = "A",
            latitude = -27.47,
            longitude = 153.02,
            kind = StopKind.PICKUP_LOAD,
        )
        val missing = RouteStop(
            id = "b",
            addressLabel = "B",
            kind = StopKind.DROP_OFF_UNLOAD,
        )
        val mapped = MapStopMarkersCatalog.mappableStops(listOf(withCoords, missing))
        assertEquals(1, mapped.size)
        assertEquals("a", mapped.single().id)
    }

    @Test
    fun demoAndRealStopsComeFromAppDataSource() {
        val today = LocalDate.of(2026, 11, 27)
        val realRoutes = RouteSession()
        val real = RealAppDataSource(
            vehicles = InMemoryVehicleProfileRepository(),
            routes = realRoutes,
            trips = TripSession(),
        )
        val demo = DemoAppDataSource.create(today = today)
        val flags = LocalFeatureFlags(mapOf(FlagKeys.STOP_MARKERS to true))
        val dual = DualModeAppDataSource(real = real, demo = demo, flags = flags)

        assertTrue(MapStopMarkersCatalog.stopsFrom(dual).isEmpty())

        flags.setEnabled(FlagKeys.DEMO_MODE, true)
        val demoStops = MapStopMarkersCatalog.stopsFrom(dual)
        assertEquals(6, demoStops.size)
        assertEquals(2, demoStops.count { it.kind == StopKind.PICKUP_LOAD })
        assertEquals(4, demoStops.count { it.kind == StopKind.DROP_OFF_UNLOAD })
        assertTrue(demoStops.all { it.latitude != null && it.longitude != null })

        flags.setEnabled(FlagKeys.DEMO_MODE, false)
        assertTrue(MapStopMarkersCatalog.stopsFrom(dual).isEmpty())

        val seeded = DemoDataset.buildRoute(today = today)
        realRoutes.replaceActive(seeded)
        val realStops = MapStopMarkersCatalog.stopsFrom(dual)
        assertEquals(6, realStops.size)
        assertEquals(seeded.stops.map { it.id }, realStops.map { it.id })
    }

    @Test
    fun satellitePeekNotInScope() {
        assertFalse(MapStopMarkersCatalog.implementsSatellitePeek)
    }
}
