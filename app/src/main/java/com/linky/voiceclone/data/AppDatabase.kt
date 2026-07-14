package com.linky.voiceclone.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Voice::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceDao(): VoiceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voices ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
