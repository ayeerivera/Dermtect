// com/example/dermtect/ui/viewmodel/DermaHistoryViewModel.kt
package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.dermtect.ui.components.DermaCaseData
import com.google.firebase.auth.FirebaseAuth

enum class DermaFeed { ALL_CASES, MY_CASES, PENDING_ONLY }

data class DermaHistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<DermaCaseData> = emptyList()
)

class DermaHistoryViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val feed: DermaFeed = DermaFeed.ALL_CASES
) : ViewModel() {

    private val _state = MutableStateFlow(DermaHistoryUiState())
    val state: StateFlow<DermaHistoryUiState> = _state

    init { refresh() }
// com/example/dermtect/ui/viewmodel/DermaHistoryViewModel.kt

    fun reloadCases() {
        // just reuse the existing refresh logic
        refresh()
    }


    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            var q: Query = db.collection("lesion_case")

            when (feed) {
                DermaFeed.MY_CASES -> if (uid != null) {
                    q = q.whereEqualTo("assessor_id", uid)
                }
                DermaFeed.PENDING_ONLY -> {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    // ✅ CORRECT: Filter by BOTH assessor ID AND status
                    q = q.whereEqualTo("assessor_id", uid)
                        .whereEqualTo("derma_status", "derma_pending")
                }
                else -> { /* ALL_CASES: no extra filter */ }
            }

            // helpful ordering (UI will still re-sort)
            q = q.orderBy("assessed_at", Query.Direction.DESCENDING)

            val snap = q.get().await()
            val items = snap.documents.map { d -> d.toDermaCaseData() }

            _state.value = DermaHistoryUiState(loading = false, items = items)
        } catch (t: Throwable) {
            _state.value = DermaHistoryUiState(loading = false, error = t.message ?: "Load error")
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toDermaCaseData(): DermaCaseData {
        // createdAt: use assessed_at if present; else original timestamp if you have one
        val assessedAtMs = getTimestamp("assessed_at")?.toDate()?.time
        val createdMs = assessedAtMs ?: getTimestamp("timestamp")?.toDate()?.time ?: 0L

        return DermaCaseData(
            caseId   = id,
            label    = getString("label") ?: "Scan",
            result   = getString("diagnosis"),        // "Benign"/"Malignant"
            date     = createdMs.formatAsDate(),      // "MMM dd, yyyy • HH:mm"
            status   = getString("derma_status"),           // "derma_completed"/"derma_pending"
            imageUrl = getString("scan_url"),
            createdAt= createdMs,
            heatmapUrl = getString("heatmap_url")
        )
    }

    private fun Long.formatAsDate(): String {
        if (this <= 0L) return "—"
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(this))
    }
}