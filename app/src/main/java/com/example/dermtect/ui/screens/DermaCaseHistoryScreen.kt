package com.example.dermtect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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

// DermaHistoryScreen
@Composable
fun DermaHistoryScreen(
    navController: NavController
) {
    // ✅ Single ViewModel instance with your factory
    val vm: DermaHistoryViewModel = viewModel(
        factory = DermaHistoryVmFactory(DermaFeed.ALL_CASES)
    )
    val uiState by vm.state.collectAsState()

    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var resultFilter by rememberSaveable { mutableStateOf(ResultFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }

    // 👇 Listen for refresh flag from Assessment screen
    val refreshFlag = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("refresh_history")
        ?.observeAsState()

    LaunchedEffect(refreshFlag?.value) {
        if (refreshFlag?.value == true) {
            // 🔄 Use the SAME vm
            vm.reloadCases()

            // clear the flag so we don't loop
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("refresh_history", false)
        }
    }

    when {
        uiState.loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        uiState.error != null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(uiState.error ?: "Error")
        }

        else -> {
            // 1. ASSIGN PERMANENT CHRONOLOGICAL NUMBER (Global numbering)
            // Ensure the base list is sorted chronologically (Oldest to Newest) to establish Scan #1, #2, etc.
            val masterChronologicalList = uiState.items
                .sortedBy { it.createdAt }

            val chronologicallyNumberedCases = masterChronologicalList.mapIndexed { index, dermacaseData ->
                IndexedCase(
                    case = dermacaseData,
                    scanNumber = index + 1 // Permanent chronological number (1, 2, 3, ...)
                )
            }

            // 2. APPLY FILTERING (Using explicit parameter 'numberedCase')
            val filteredByStatus = when (statusFilter) {
                StatusFilter.ALL -> chronologicallyNumberedCases
                StatusFilter.PENDING -> chronologicallyNumberedCases.filter { numberedCase ->
                    numberedCase.case.status?.lowercase() != "derma_completed"
                }
                StatusFilter.COMPLETED -> chronologicallyNumberedCases.filter { numberedCase ->
                    numberedCase.case.status?.lowercase() == "derma_completed"
                }
            }

            val filteredByResult = when (resultFilter) {
                ResultFilter.ALL -> filteredByStatus
                ResultFilter.BENIGN -> filteredByStatus.filter { numberedCase ->
                    numberedCase.case.result?.lowercase() == "benign"
                }
                ResultFilter.MALIGNANT -> filteredByStatus.filter { numberedCase ->
                    numberedCase.case.result?.lowercase() == "malignant"
                }
            }

            // 3. APPLY SORTING (The scanNumber remains fixed)
            val sorted = filteredByResult
                .sortedBy { numberedCase -> numberedCase.case.createdAt } // Sort the IndexedCase list by the case's createdAt
                .let { if (newestFirst) it.reversed() else it }

            // 4. EXTRACT the case data and numbers for the template
            val finalCaseList = sorted.map { it.case }
            val finalScanNumbers = sorted.map { it.scanNumber }

            // Compute counts for Home
            val totalForHome = sorted.size
            val pendingForHome = sorted.count { it.case.status.equals("derma_pending", true) }

            // ✅ Push to Home’s SavedStateHandle (Existing logic)
            LaunchedEffect(totalForHome, pendingForHome) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("home_total_count", totalForHome)

                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("home_pending_count", pendingForHome)
            }

            DermaHistoryScreenTemplate(
                navController = navController,
                screenTitle = "History Cases",
                caseList = finalCaseList,    // Pass the CaseData list
                scanNumbers = finalScanNumbers, // Pass the corresponding fixed numbers
                showIndicators = true,
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
                },
                isDerma = true
            )
        }
    }
}