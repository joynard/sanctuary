package com.sanctuary.core.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sanctuary.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(
    name = "appearance_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class AppearancePreferences(private val context: Context) {
    val themeMode: Flow<ThemeMode> = context.appearanceDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> ThemeMode.fromKey(prefs[THEME_MODE]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appearanceDataStore.edit { it[THEME_MODE] = mode.key }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
