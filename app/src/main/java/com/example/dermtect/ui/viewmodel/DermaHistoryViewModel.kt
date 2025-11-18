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
    private val feed: DermaFeed = DermaFeed.MY_CASES   // ✅ default to MY_CASES
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

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (user == null || uid == null) {
            _state.value = DermaHistoryUiState(loading = false, error = "User not logged in or session expired.")
            return@launch
        }

        try {
            // 🛑 IMPORTANT: Use "lesion_case" based on your rules, not "cases" if that's what you used previously.
            var query: Query = db.collection("lesion_case")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)

            // 🚀 CRITICAL FIX: Add the security filter based on the feed
            query = when (feed) {
                DermaFeed.MY_CASES -> {
                    // Must be filtered by assessor_id to satisfy your rule's requirement for 'list'
                    query.whereEqualTo("assessor_id", uid)
                }
                DermaFeed.PENDING_ONLY -> {
                    // 🚀 FIX: Add a public access filter to secure the query scope
                    query.whereEqualTo("derma_status", "derma_pending")
                        .whereEqualTo("public_allowed", true)
                }
                DermaFeed.ALL_CASES -> {
                     query.whereArrayContains("opened_by", uid) // Cases this derma has at least opened once
                }
            }

            val snap = query.get().await() // Execute the filtered query
            val items = snap.documents.map { d -> d.toDermaCaseData() }

            _state.value = DermaHistoryUiState(
                loading = false,
                items = items
            )
        } catch (t: Throwable) {
            _state.value = DermaHistoryUiState(
                loading = false,
                error = t.message ?: "Load error"
            )
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