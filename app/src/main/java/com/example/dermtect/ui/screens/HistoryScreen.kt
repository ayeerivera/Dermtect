package com.example.dermtect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(navController: NavController, vm: HistoryViewModel = viewModel()) {
    val uiState by vm.state.collectAsState()

    when {
        uiState.loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error")
            }
        }
        else -> {
            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "Scan History",
                caseList = uiState.items
            )
        }
    }
}