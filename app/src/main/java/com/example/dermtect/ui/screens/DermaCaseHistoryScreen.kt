package com.example.dermtect.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.dermtect.ui.components.CaseData
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.R

@Composable
fun CaseHistoryScreen(navController: NavController) {
    val allCases = listOf(
        CaseData(R.drawable.sample_skin_1, "Scan 1", "Pending", "May 8, 2025", "Pending"),
        CaseData(R.drawable.sample_skin_1, "Scan 2", "Benign", "May 9, 2025", "Completed"),
        CaseData(R.drawable.sample_skin_1, "Scan 3", "Malignant", "May 10, 2025", "Completed")
    )
    HistoryScreenTemplate(navController, "Case History", allCases)
}


