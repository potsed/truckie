package com.truckie1.app.profiles

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * On-device Room database for vehicles + active tour routes/stops (ATNA-89).
 *
 * Version 2 adds route persistence. [MIGRATION_1_2] is the primary upgrade path;
 * destructive fallback is **not** the sole migration story.
 * Production builder does **not** [allowMainThreadQueries] — DAOs are suspend.
 *
 * Schema JSON is exported under `app/schemas/` (`exportSchema = true`).
 */
@Database(
    entities = [
        VehicleEntity::class,
        ActiveVehicleEntity::class,
        RouteEntity::class,
        RouteStopEntity::class,
        RouteCargoEntity::class,
        ActiveTourMetaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun activeTourDao(): ActiveTourDao

    companion object {
        /** Schema version with routes/stops/cargo/active_tour_meta (ATNA-89). */
        const val VERSION: Int = 2
        /**
         * Vehicles-only v1 → v2 with routes / stops / cargo / active_tour_meta.
         * Existing vehicle rows are preserved.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT,
                        `dateEpochDay` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `startAddress` TEXT,
                        `startLatitude` REAL,
                        `startLongitude` REAL,
                        `endAddress` TEXT,
                        `endLatitude` REAL,
                        `endLongitude` REAL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `route_stops` (
                        `id` TEXT NOT NULL,
                        `routeId` TEXT NOT NULL,
                        `addressLabel` TEXT NOT NULL,
                        `latitude` REAL,
                        `longitude` REAL,
                        `kind` TEXT NOT NULL,
                        `stopOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_route_stops_routeId` ON `route_stops` (`routeId`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `route_cargo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stopId` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `heightM` REAL NOT NULL,
                        `widthM` REAL NOT NULL,
                        `lengthM` REAL NOT NULL,
                        `massT` REAL NOT NULL,
                        `action` TEXT NOT NULL,
                        `cargoOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`stopId`) REFERENCES `route_stops`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_route_cargo_stopId` ON `route_cargo` (`stopId`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_tour_meta` (
                        `id` INTEGER NOT NULL,
                        `activeRouteId` TEXT,
                        `lastOptimizeMode` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context, name: String = "truckie_vehicles.db"): VehicleDatabase =
            Room.databaseBuilder(context.applicationContext, VehicleDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()

        /**
         * In-memory DB for JVM / instrumented tests. Still no main-thread allow —
         * repositories hop to IO via suspend DAOs.
         */
        fun inMemory(context: Context): VehicleDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, VehicleDatabase::class.java)
                .build()
    }
}
