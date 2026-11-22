package com.truckie1.app.profiles

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class, ActiveVehicleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    companion object {
        fun build(context: Context, name: String = "truckie_vehicles.db"): VehicleDatabase =
            Room.databaseBuilder(context.applicationContext, VehicleDatabase::class.java, name)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

        fun inMemory(context: Context): VehicleDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, VehicleDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
