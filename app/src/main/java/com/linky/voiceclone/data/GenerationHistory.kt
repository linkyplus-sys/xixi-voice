package com.linky.voiceclone.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "generation_history",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["outputFileName"], unique = true),
    ],
)
data class GenerationHistory(
    @PrimaryKey val id: String,
    val text: String,
    val voiceId: String?,
    val voiceName: String,
    val mode: String,
    val outputFileName: String,
    val format: String,
    val fileSize: Long,
    val durationMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "completed",
    val errorMessage: String? = null,
)
