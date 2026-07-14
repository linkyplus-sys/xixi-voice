package com.linky.voiceclone.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voices ORDER BY CASE WHEN lastUsedAt > 0 THEN 0 ELSE 1 END, lastUsedAt DESC, createdAt DESC")
    fun getAll(): Flow<List<Voice>>

    @Query("SELECT * FROM voices WHERE id = :id")
    suspend fun getById(id: String): Voice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voice: Voice)

    @Delete
    suspend fun delete(voice: Voice)

    @Update
    suspend fun update(voice: Voice)

    @Query("UPDATE voices SET lastUsedAt = :time WHERE id = :id")
    suspend fun updateLastUsed(id: String, time: Long = System.currentTimeMillis())
}
