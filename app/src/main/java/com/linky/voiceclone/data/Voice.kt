package com.linky.voiceclone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voices")
data class Voice(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val sampleFileName: String,
    val sampleSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0  // 最近一次用于合成的时间
)
