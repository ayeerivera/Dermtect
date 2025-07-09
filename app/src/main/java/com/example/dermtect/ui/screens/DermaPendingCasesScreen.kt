package com.example.dermtect.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.dermtect.ui.components.CaseData
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.R

@Composable
fun PendingCasesScreen(navController: NavController) {
    val pendingCases = listOf(
        CaseData(R.drawable.sample_skin_1, "Scan 1", null, "May 8, 2025", null),
        CaseData(R.drawable.sample_skin_1, "Scan 2", null, "May 9, 2025", null)
    )
    HistoryScreenTemplate(navController, "Pending Cases", pendingCases)
}

