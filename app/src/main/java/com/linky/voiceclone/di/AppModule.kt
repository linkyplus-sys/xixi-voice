package com.linky.voiceclone.di

import android.content.Context
import androidx.room.Room
import com.linky.voiceclone.data.AppDatabase
import com.linky.voiceclone.data.SettingsDataStore
import com.linky.voiceclone.data.VoiceDao
import com.linky.voiceclone.data.GenerationHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "voiceclone.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideVoiceDao(db: AppDatabase): VoiceDao = db.voiceDao()

    @Provides
    fun provideGenerationHistoryDao(db: AppDatabase): GenerationHistoryDao =
        db.generationHistoryDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext ctx: Context): SettingsDataStore =
        SettingsDataStore(ctx)
}
