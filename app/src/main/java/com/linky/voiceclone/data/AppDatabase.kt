package com.linky.voiceclone.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Voice::class, GenerationHistory::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceDao(): VoiceDao
    abstract fun generationHistoryDao(): GenerationHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voices ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS generation_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        text TEXT NOT NULL,
                        voiceId TEXT,
                        voiceName TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        outputFileName TEXT NOT NULL,
                        format TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        durationMs INTEGER,
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        errorMessage TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_generation_history_createdAt " +
                        "ON generation_history (createdAt)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_generation_history_outputFileName " +
                        "ON generation_history (outputFileName)",
                )
            }
        }
    }
}
