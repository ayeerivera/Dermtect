// TutorialManager.kt (Updated Version)
package com.example.dermtect.ui.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

data class TutorialStep(
    val key: String,
    val description: String
)

class TutorialManager {
    var currentStep by mutableStateOf(0)
    var currentTargetBounds: Rect? by mutableStateOf(null)

    val steps = listOf(
        TutorialStep("notification", "Tap here to check your notifications."),
        TutorialStep("skin_report", "View your previous skin scan results here."),
        TutorialStep("nearby_clinics", "Find clinics and dermatologists near you."),
        TutorialStep("highlight_card", "Read important highlights and health tips here."),
        TutorialStep("news_carousel", "Swipe to explore the latest skincare news and articles."),
        TutorialStep("camera", "Tap this button to take a photo for skin analysis."),
        TutorialStep("home", "Go back to your main dashboard anytime."),
        TutorialStep("settings", "Manage your account, profile, and app settings here.")

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
}