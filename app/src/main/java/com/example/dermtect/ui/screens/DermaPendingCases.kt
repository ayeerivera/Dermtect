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
import com.example.dermtect.ui.components.DermaHistoryScreenTemplate
import com.example.dermtect.ui.components.IndexedCase
import com.example.dermtect.ui.viewmodel.DermaFeed
import com.example.dermtect.ui.viewmodel.DermaHistoryViewModel
import com.example.dermtect.ui.viewmodel.DermaHistoryVmFactory

// com/example/dermtect/ui/screens/PendingCasesScreen.kt
@Composable
fun PendingCasesScreen(
    navController: NavController,
) {
    // ✅ STEP 1: CHANGE FACTORY to load ALL_CASES to establish a global number
    val vm: DermaHistoryViewModel = viewModel(
        factory = DermaHistoryVmFactory(DermaFeed.PENDING_ONLY)
    )

    val uiState by vm.state.collectAsState()

    var newestFirst by rememberSaveable { mutableStateOf(true) }
    // ...
    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error ?: "Error") }
        else -> {
            // 1. ASSIGN PERMANENT CHRONOLOGICAL NUMBER (Global numbering)
            // Ensure the base list is sorted chronologically (Oldest to Newest) to establish Scan #1, #2, etc.
            // 1. Sort whatever VM gives you (these are already pending)
            val masterChronologicalList = uiState.items
                .sortedBy { it.createdAt } // oldest → newest

// 2. Number them globally *within pending list* (Scan #1, #2 in Pending)
            val numberedCases = masterChronologicalList.mapIndexed { index, dermacaseData ->
                IndexedCase(
                    case = dermacaseData,
                    scanNumber = index + 1
                )
            }

// 3. Apply sort order (newestFirst toggle)
            val sorted = numberedCases
                .sortedBy { it.case.createdAt }
                .let { if (newestFirst) it.reversed() else it }

// 4. Extract for template
            val finalCaseList = sorted.map { it.case }
            val finalScanNumbers = sorted.map { it.scanNumber }

// 5. This is the REAL pending count for home
            val pendingForHome = finalCaseList.size

            LaunchedEffect(pendingForHome) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("home_pending_count", pendingForHome)
            }



            DermaHistoryScreenTemplate(
                navController = navController,
                screenTitle = "Pending Cases",
                caseList = finalCaseList,    // ✅ Use the FINAL filtered list
                scanNumbers = finalScanNumbers, // ✅ Use the FINAL filtered numbers
                showIndicators = true,
                actions = {
                    HistoryFilterButton(
                        isDerma = true,
                        statusFilter = StatusFilter.ALL,
                        onStatusChange = { /* no-op */ },
                        resultFilter = ResultFilter.ALL,
                        onResultChange = { /* no-op */ },
                        newestFirst = newestFirst,
                        onSortChange = { newestFirst = it },
                        showStatusFilters = false,
                        showResultFilters = false
                    )
                },
                isDerma = true
            )
        }
    }
}