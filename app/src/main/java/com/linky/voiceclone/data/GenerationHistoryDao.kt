package com.linky.voiceclone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationHistoryDao {
    @Query("SELECT * FROM generation_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GenerationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GenerationHistory)

    @Query("SELECT outputFileName FROM generation_history")
    suspend fun existingFileNames(): List<String>

    @Query("DELETE FROM generation_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
