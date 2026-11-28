package com.truckie1.app.profiles

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.truckie1.app.ui.RoomRoutePersistCatalog
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Gherkin: features/room_route_persist.feature — Migration 1→2 keeps vehicles.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VehicleDatabaseMigrationTest {

    @Test
    fun catalogRejectsDestructiveOnlyAndMainThreadAllow() {
        assertFalse(RoomRoutePersistCatalog.destructiveMigrationIsSoleStory)
        assertFalse(RoomRoutePersistCatalog.productionAllowsMainThreadQueries)
        assertTrue(RoomRoutePersistCatalog.exportSchemaEnabled)
        assertEquals(2, VehicleDatabase.VERSION)
    }

    @Test
    fun migration1to2KeepsVehiclesAndAddsRouteTables() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val dbName = "truckie-migrate-1-2.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `vehicles` (
                            `rego` TEXT NOT NULL,
                            `nickname` TEXT NOT NULL,
                            `vehicleType` TEXT NOT NULL,
                            `make` TEXT NOT NULL,
                            `model` TEXT NOT NULL,
                            `year` INTEGER NOT NULL,
                            `heightM` REAL NOT NULL,
                            `widthM` REAL NOT NULL,
                            `lengthM` REAL NOT NULL,
                            `massT` REAL NOT NULL,
                            `tareT` REAL NOT NULL,
                            `gvmT` REAL NOT NULL,
                            `gcmT` REAL NOT NULL,
                            `axleCount` INTEGER NOT NULL,
                            `networkClass` TEXT,
                            `notes` TEXT,
                            PRIMARY KEY(`rego`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `active_vehicle` (
                            `id` INTEGER NOT NULL,
                            `rego` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `vehicles` (
                            rego, nickname, vehicleType, make, model, year,
                            heightM, widthM, lengthM, massT, tareT, gvmT, gcmT,
                            axleCount, networkClass, notes
                        ) VALUES (
                            'ABC123', 'Blue', 'Rigid', 'Volvo', 'FM', 2019,
                            3.0, 2.5, 12.5, 22.5, 8.5, 26.0, 42.5,
                            3, 'GML', NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "INSERT INTO `active_vehicle` (id, rego) VALUES (1, 'ABC123')",
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()

        val roomDb = Room.databaseBuilder(context, VehicleDatabase::class.java, dbName)
            .addMigrations(VehicleDatabase.MIGRATION_1_2)
            .build()

        try {
            val vehicles = RoomVehicleProfileRepository(roomDb.vehicleDao())
            assertEquals("ABC123", vehicles.activeRego)
            assertEquals("Volvo", vehicles.findByRego("ABC123")!!.make)

            val tour = RoomActiveTour(roomDb.activeTourDao())
            tour.createRoute(
                name = "After migrate",
                dateChoice = RouteDateChoice.TODAY,
                today = LocalDate.of(2026, 11, 28),
                start = RouteEndpoint("Start", -27.4, 153.0),
                end = RouteEndpoint("End", -27.5, 153.1),
            )
            assertEquals("After migrate", tour.activeRoute()!!.name)

            val hydrated = RoomActiveTour(roomDb.activeTourDao())
            assertEquals("After migrate", hydrated.activeRoute()!!.name)
            assertEquals("ABC123", vehicles.activeRego)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
