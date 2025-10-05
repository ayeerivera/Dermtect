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
fun DermaHistoryScreen(navController: NavController, vm: HistoryViewModel = viewModel()) {
    val uiState by vm.state.collectAsState()

    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var resultFilter by rememberSaveable { mutableStateOf(ResultFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error ?: "Error") }
        else -> {
            val filteredByStatus = uiState.items.filter { c ->
                when (statusFilter) {
                    StatusFilter.ALL -> true
                    StatusFilter.COMPLETED -> c.status.equals("completed", true)
                    StatusFilter.PENDING -> c.status.equals("pending", true)
                }
            }
            val filteredByResult = filteredByStatus.filter { c ->
                when (resultFilter) {
                    ResultFilter.ALL -> true
                    ResultFilter.BENIGN -> c.result.equals("benign", true)
                    ResultFilter.MALIGNANT -> c.result.equals("malignant", true)
                }
            }
            val sorted = filteredByResult.sortedBy { it.createdAt }.let { if (newestFirst) it.reversed() else it }

            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "History Cases",
                caseList = sorted,
                showIndicators = true, // derma: dots allowed
                actions = {
                    HistoryFilterButton(
                        isDerma = true,
                        statusFilter = statusFilter,
                        onStatusChange = { statusFilter = it },
                        resultFilter = resultFilter,
                        onResultChange = { resultFilter = it },
                        newestFirst = newestFirst,
                        onSortChange = { newestFirst = it }
                    )
                }
            )
        }
    }
}
