package com.example.dermtect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.example.dermtect.ui.viewmodel.HistoryViewModel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import com.example.dermtect.ui.components.HistoryFilterButton

enum class StatusFilter { ALL, COMPLETED, PENDING }
enum class ResultFilter { ALL, BENIGN, MALIGNANT }

@Composable
fun HistoryScreen(navController: NavController, vm: HistoryViewModel = viewModel()) {
    val uiState by vm.state.collectAsState()

    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var resultFilter by rememberSaveable { mutableStateOf(ResultFilter.ALL) } // ignored for user
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
            val sorted = filteredByStatus.sortedBy { it.createdAt }.let {
                if (newestFirst) it else it.reversed()
            }

            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "Scan History",
                caseList = sorted,
                showIndicators = false, // user: no dots, unrelated to filters but keeping your rule
                actions = {
                    HistoryFilterButton(
                        isDerma = false,
                        statusFilter = statusFilter,
                        onStatusChange = { statusFilter = it },
                        resultFilter = resultFilter,
                        onResultChange = { /* no-op for user */ },
                        newestFirst = newestFirst,
                        onSortChange = { newestFirst = it }
                    )
                }
            )
        }
    }
}


