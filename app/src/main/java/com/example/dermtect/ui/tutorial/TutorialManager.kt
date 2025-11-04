// TutorialManager.kt (Updated Version)
package com.example.dermtect.ui.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tutorial_prefs")
private val KEY_FIRST_RUN = booleanPreferencesKey("first_run")

data class TutorialStep(
    val key: String,
    val description: String
)

class TutorialManager {
    var currentStep by mutableStateOf(0)
    var currentTargetBounds: Rect? by mutableStateOf(null)
    var isFirstRun by mutableStateOf(true)
        private set
    val steps = listOf(
        TutorialStep("profile_menu", "Tap here for your profile menu (Edit Profile, Assessment, Notifications, and more)."),
        TutorialStep("skin_report", "View your previous skin scan results here."),
        TutorialStep("nearby_clinics", "Find clinics and dermatologists near you."),
        TutorialStep("highlight_card", "Read important highlights and health tips here."),
        TutorialStep("news_carousel", "Swipe to explore the latest skincare news and articles."),
        TutorialStep("camera", "Tap this button to take a photo for skin analysis.")

    )


    fun getStepKey(): String = steps.getOrNull(currentStep)?.key ?: ""

    fun nextStep() {
        currentTargetBounds = null

        if (currentStep < steps.lastIndex) {
            currentStep++
        } else {
            // Mark finished
            currentStep = steps.size
        }
    }

    // ✅ ADDED previousStep function
    fun previousStep() {
        currentTargetBounds = null
        if (currentStep > 0) {
            currentStep--
        }
    }

    fun isFinished(): Boolean = currentStep >= steps.size
    // --- Persistence lives here ---
    suspend fun initialize(context: Context) {
        // Read first-run flag (default true)
        isFirstRun = context.dataStore.data.map { it[KEY_FIRST_RUN] ?: true }.first()
    }

    suspend fun markSeen(context: Context) {
        context.dataStore.edit { it[KEY_FIRST_RUN] = false }
        isFirstRun = false
    }
}