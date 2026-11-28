package com.truckie1.app.profiles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Suspend vehicle DAO — no main-thread Room queries (ATNA-89).
 */
@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE rego = :rego LIMIT 1")
    suspend fun findByRego(rego: String): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY nickname COLLATE NOCASE ASC")
    suspend fun all(): List<VehicleEntity>

    @Query("SELECT rego FROM active_vehicle WHERE id = 1 LIMIT 1")
    suspend fun activeRego(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActiveRow(row: ActiveVehicleEntity)

    @Query("DELETE FROM active_vehicle")
    suspend fun clearActive()

    @Transaction
    suspend fun saveAndMaybeActivate(entity: VehicleEntity, activateIfNone: Boolean) {
        upsert(entity)
        val current = activeRego()
        if (activateIfNone && current == null) {
            setActiveRow(ActiveVehicleEntity(rego = entity.rego))
        }
    }
}
