package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.trips.ActiveTourSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Gherkin: features/room_route_persist.feature — catalog / flag contracts (ATNA-89).
 */
class RoomRoutePersistCatalogTest {

    @Test
    fun flagKeyAndDefaultsOn() {
        assertEquals("ff_room_route_persist", FlagKeys.ROOM_ROUTE_PERSIST)
        assertEquals(FlagKeys.ROOM_ROUTE_PERSIST, RoomRoutePersistCatalog.FLAG_KEY)
        assertTrue(RoomRoutePersistCatalog.DEFAULT_ENABLED)
        assertTrue(RoomRoutePersistCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(RoomRoutePersistCatalog.exportSchemaEnabled)
        assertFalse(RoomRoutePersistCatalog.productionAllowsMainThreadQueries)
        assertFalse(RoomRoutePersistCatalog.destructiveMigrationIsSoleStory)
        assertEquals(2, RoomRoutePersistCatalog.databaseVersionWithRoutes)
    }

    @Test
    fun flagToggle() {
        val flags = LocalFeatureFlags()
        assertFalse(RoomRoutePersistCatalog.isEnabled(flags))
        RoomRoutePersistCatalog.setEnabled(flags, true)
        assertTrue(RoomRoutePersistCatalog.isEnabled(flags))
    }

    @Test
    fun realSourceSharesSingleTourFacade() {
        val real = RealAppDataSource(vehicles = InMemoryVehicleProfileRepository())
        assertTrue(ActiveTourCatalog.sharesSingleTour(real))
        assertSame(real.routes, real.trips)
        assertTrue(RoomRoutePersistCatalog.isInMemorySession(real.tour))
    }

    @Test
    fun demoStaysReadOnlyInMemorySeed() {
        val demo = DemoAppDataSource.create(today = LocalDate.of(2026, 11, 28))
        assertTrue(ActiveTourCatalog.sharesSingleTour(demo))
        assertSame(demo.routes, demo.trips)
        assertTrue(demo.routes is ReadOnlyActiveTour)
        assertFalse(RoomRoutePersistCatalog.isRoomBacked(demo.routes as ReadOnlyActiveTour))
    }

    @Test
    fun inMemorySessionDetection() {
        assertTrue(RoomRoutePersistCatalog.isInMemorySession(ActiveTourSession()))
    }
}
