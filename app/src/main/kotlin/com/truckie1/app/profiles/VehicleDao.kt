package com.truckie1.app.profiles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE rego = :rego LIMIT 1")
    fun findByRego(rego: String): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY nickname COLLATE NOCASE ASC")
    fun all(): List<VehicleEntity>

    @Query("SELECT rego FROM active_vehicle WHERE id = 1 LIMIT 1")
    fun activeRego(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setActiveRow(row: ActiveVehicleEntity)

    @Query("DELETE FROM active_vehicle")
    fun clearActive()

    @Transaction
    fun saveAndMaybeActivate(entity: VehicleEntity, activateIfNone: Boolean) {
        upsert(entity)
        val current = activeRego()
        if (activateIfNone && current == null) {
            setActiveRow(ActiveVehicleEntity(rego = entity.rego))
        }
    }
}
