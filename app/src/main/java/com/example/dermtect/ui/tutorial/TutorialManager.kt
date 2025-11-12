package com.example.dermtect.ui.tutorial

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Patient tutorial datastore (kept for default manager)
private val Context.patientDataStore by preferencesDataStore(name = "tutorial_prefs")
private val KEY_FIRST_RUN = booleanPreferencesKey("first_run")

data class TutorialStep(
    val key: String,
    val description: String
)

open class TutorialManager {
    var currentStep by mutableStateOf(0)
    var currentTargetBounds: Rect? by mutableStateOf(null)
    var isFirstRun by mutableStateOf(true)
        protected set

    // Default (patient) steps; derma will override
    open val steps: List<TutorialStep> = listOf(
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
        if (currentStep < steps.lastIndex) currentStep++ else currentStep = steps.size
    }

    fun previousStep() {
        currentTargetBounds = null
        if (currentStep > 0) currentStep--
    }

    fun isFinished(): Boolean = currentStep >= steps.size

    open suspend fun initialize(context: Context) {
        isFirstRun = context.patientDataStore.data.map { it[KEY_FIRST_RUN] ?: true }.first()
    }

    open suspend fun markSeen(context: Context) {
        context.patientDataStore.edit { it[KEY_FIRST_RUN] = false }
        isFirstRun = false
    }
}
