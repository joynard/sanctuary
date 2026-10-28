package com.sanctuary.core.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.trackingDataStore by preferencesDataStore(
    name = "tracking_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Device-scoped switches that must be readable by the accessibility service even
 * before the UI is opened. Profile data itself stays in the database.
 */
class TrackingPreferences(private val context: Context) {
    val paused: Flow<Boolean> = context.trackingDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PAUSED] ?: false }

    suspend fun setPaused(value: Boolean) {
        context.trackingDataStore.edit { it[PAUSED] = value }
    }

    suspend fun deviceId(): String {
        val existing = context.trackingDataStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        context.trackingDataStore.edit { prefs ->
            // First writer wins if two coroutines race at first launch.
            if (prefs[DEVICE_ID].isNullOrBlank()) prefs[DEVICE_ID] = generated
        }
        return context.trackingDataStore.data.first()[DEVICE_ID] ?: generated
    }

    private companion object {
        val PAUSED = booleanPreferencesKey("tracking_paused")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }
}
