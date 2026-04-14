package com.blind.social.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val AUTO_READ_MESSAGES = booleanPreferencesKey("auto_read_messages")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val IS_DESIGN_2 = booleanPreferencesKey("is_design_2")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE] ?: false // Default to light mode
        }

    val isDesign2: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DESIGN_2] ?: true // Default to Design 2
        }

    val autoReadMessages: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_READ_MESSAGES] ?: false // Default to kapalı
        }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAPTIC_FEEDBACK] ?: true // Default to açık
        }

    suspend fun saveThemePreference(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun saveAutoReadPreference(autoRead: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_READ_MESSAGES] = autoRead
        }
    }

    suspend fun saveHapticPreference(haptic: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = haptic
        }
    }

    suspend fun saveDesignPreference(isDesign2: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DESIGN_2] = isDesign2
        }
    }
}