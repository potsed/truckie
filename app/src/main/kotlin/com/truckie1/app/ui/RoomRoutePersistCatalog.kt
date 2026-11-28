package com.truckie1.app.ui

import com.truckie1.app.profiles.RoomActiveTour
import com.truckie1.app.profiles.VehicleDatabase
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.ActiveTourFacade
import com.truckie1.trips.ActiveTourSession

/**
 * Room-persist active route + stops (`ff_room_route_persist`, ATNA-89 / P1-4).
 *
 * Real [AppDataSource] uses [RoomActiveTour] as the single ActiveTour SoT.
 * Demo stays a separate read-only in-memory seed. Defaults **on**.
 * In-memory-only real tour path is deleted when the flag defaults on.
 *
 * Gherkin: features/room_route_persist.feature
 */
object RoomRoutePersistCatalog {
    const val FLAG_KEY: String = FlagKeys.ROOM_ROUTE_PERSIST

    /** Ship / dogfood default after ATNA-89. */
    const val DEFAULT_ENABLED: Boolean = true

    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    const val exportSchemaEnabled: Boolean = true
    const val productionAllowsMainThreadQueries: Boolean = false
    const val destructiveMigrationIsSoleStory: Boolean = false
    const val databaseVersionWithRoutes: Int = VehicleDatabase.VERSION

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ROOM_ROUTE_PERSIST)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.ROOM_ROUTE_PERSIST, enabled)
    }

    /** Real tour must be Room-backed when the flag defaults on. */
    fun isRoomBacked(tour: ActiveTourFacade): Boolean = tour is RoomActiveTour

    /** Demo must stay in-memory ActiveTourSession under ReadOnlyActiveTour. */
    fun isInMemorySession(tour: ActiveTourFacade): Boolean =
        tour is ActiveTourSession || tour is ReadOnlyActiveTour
}
