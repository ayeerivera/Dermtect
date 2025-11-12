package com.example.dermtect.ui.tutorial

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dermaDataStore by preferencesDataStore(name = "derma_tutorial_prefs")
private val KEY_FIRST_RUN_DERMA = booleanPreferencesKey("first_run_derma")

class DermaTutorialManager : TutorialManager() {

    override val steps = listOf(
        TutorialStep(
            "derma_profile_menu",
            "Open your profile menu here to access Notifications, Edit Profile, About DermTect, and Log Out."
        ),
        TutorialStep(
            "pending_cases_tab",
            "View all cases currently waiting for your assessment."
        ),
        TutorialStep(
            "case_history_tab",
            "Browse through your previously assessed cases. You can filter them by date, status, or result."
        ),
        TutorialStep(
            "search_bar",
            "Quickly find a case by entering its Report ID in the search bar."
        ),
        TutorialStep(
            "pending_cases_highlight",
            "Your newest pending case appears here for quick access."
        ),
        TutorialStep(
            "camera_scanner",
            "Capture a lesion photo for assessment."
        )
    )


    override suspend fun initialize(context: Context) {
        isFirstRun = context.dermaDataStore.data.map { it[KEY_FIRST_RUN_DERMA] ?: true }.first()
    }

    override suspend fun markSeen(context: Context) {
        context.dermaDataStore.edit { it[KEY_FIRST_RUN_DERMA] = false }
        isFirstRun = false
    }
}
