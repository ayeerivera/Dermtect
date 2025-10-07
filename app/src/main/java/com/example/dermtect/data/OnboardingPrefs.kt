package com.example.dermtect.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

object OnboardingPrefs {
    private val KEY_SEEN = booleanPreferencesKey("seen_onboarding")

    // Check if onboarding has been seen
    suspend fun hasSeen(context: Context): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SEEN] ?: false
        }.first()
    }

    // Mark onboarding as seen
    suspend fun setSeen(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEEN] = true   // ✅ works now because prefs is MutablePreferences
        }
    }
}
