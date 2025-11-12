// com/example/dermtect/ui/screens/PendingCasesScreen.kt
package com.example.dermtect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dermtect.ui.components.HistoryFilterButton
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.ui.viewmodel.DermaFeed
import com.example.dermtect.ui.viewmodel.DermaHistoryViewModel
import com.example.dermtect.ui.viewmodel.DermaHistoryVmFactory
import com.example.dermtect.ui.viewmodel.HistoryViewModel

// com/example/dermtect/ui/screens/PendingCasesScreen.kt
@Composable
fun PendingCasesScreen(
    navController: NavController,
) {
    val vm: DermaHistoryViewModel = viewModel(
        factory = DermaHistoryVmFactory(DermaFeed.PENDING_ONLY)
    )
    val uiState by vm.state.collectAsState()

    var newestFirst by rememberSaveable { mutableStateOf(true) }

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error ?: "Error") }
        else -> {
            val sorted = uiState.items
                .sortedBy { it.createdAt }
                .let { if (newestFirst) it.reversed() else it }

            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "Pending Cases",
                caseList = sorted,
                showIndicators = true,
                actions = {
                    HistoryFilterButton(
                        isDerma = false,
                        statusFilter = StatusFilter.ALL,
                        onStatusChange = { /* no-op for pending-only feed */ },
                        resultFilter = ResultFilter.ALL,
                        onResultChange = { /* no-op for pending-only feed */ },
                        newestFirst = newestFirst,
                        onSortChange = { newestFirst = it }
                    )
                },
                        isDerma = true                  // ← hides “Take your first scan” button + derma copy

            )
        }
    }
}