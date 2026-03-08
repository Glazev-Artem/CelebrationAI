package com.glazev.celebrationai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CelebrationDao {
    @Query("SELECT * FROM celebrations ORDER BY date ASC")
    fun getAllCelebrations(): Flow<List<Celebration>>

    @Query("SELECT * FROM celebrations ORDER BY date ASC")
    suspend fun getAllCelebrationsSync(): List<Celebration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCelebration(celebration: Celebration): Long

    @Delete
    suspend fun deleteCelebration(celebration: Celebration): Int

    @Query("SELECT * FROM celebrations WHERE id = :id")
    suspend fun getCelebrationById(id: Int): Celebration?
}
