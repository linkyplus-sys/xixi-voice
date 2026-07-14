package com.linky.voiceclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val baseUrl: Flow<String> = context.dataStore.data.map {
        it[BASE_URL] ?: "https://token-plan-cn.xiaomimimo.com/v1"
    }

    suspend fun setApiKey(key: String) { context.dataStore.edit { it[API_KEY] = key } }
    suspend fun setBaseUrl(url: String) { context.dataStore.edit { it[BASE_URL] = url } }
}
