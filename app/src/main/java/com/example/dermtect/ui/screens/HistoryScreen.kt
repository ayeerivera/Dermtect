package com.example.dermtect.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.dermtect.ui.components.CaseData
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.R

@Composable
fun HistoryScreen(navController: NavController) {
    val userCases = listOf(
        CaseData(R.drawable.sample_skin_1, "Scan 1", "Benign", "May 10, 2025", null),
        CaseData(R.drawable.sample_skin_1, "Scan 2", "Pending", "May 10, 2025", null)
    )
    HistoryScreenTemplate(navController, "History", userCases)
}

