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
import com.example.dermtect.ui.viewmodel.HistoryViewModel

@Composable
fun PendingCasesScreen(navController: NavController, vm: HistoryViewModel = viewModel()) {
    val uiState by vm.state.collectAsState()

    var newestFirst by rememberSaveable { mutableStateOf(true) }

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error ?: "Error") }
        else -> {
            val pendingOnly = uiState.items.filter { it.status.equals("pending", true) }
            val sorted = pendingOnly.sortedBy { it.createdAt }.let { if (newestFirst) it.reversed() else it }

            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "Pending Cases",
                caseList = sorted,
                showIndicators = true, // keep dots
                actions = {
                    HistoryFilterButton(
                        isDerma = false, // no status/result filter here
                        statusFilter = StatusFilter.ALL,
                        onStatusChange = { },
                        resultFilter = ResultFilter.ALL,
                        onResultChange = { },
                        newestFirst = newestFirst,
                        onSortChange = { newestFirst = it }
                    )
                }
            )
        }
    }
}
