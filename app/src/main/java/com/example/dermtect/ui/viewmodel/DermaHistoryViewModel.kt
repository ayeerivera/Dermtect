// com/example/dermtect/ui/viewmodel/DermaHistoryViewModel.kt
package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dermtect.ui.components.CaseData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class DermaHistoryUiState(
    val loading: Boolean = true,
    val items: List<CaseData> = emptyList(),
    val error: String? = null
)

enum class DermaFeed { PENDING_ONLY, ALL_CASES }

class DermaHistoryViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val feed: DermaFeed = DermaFeed.PENDING_ONLY
) : ViewModel() {

    val state = MutableStateFlow(DermaHistoryUiState())

    init { observeCases() }

    private fun observeCases() {
        viewModelScope.launch {
            val flow = when (feed) {
                DermaFeed.PENDING_ONLY -> casesFlow(wherePending = true)
                DermaFeed.ALL_CASES    -> casesFlow(wherePending = false)
            }
            flow.collect { list ->
                state.update { it.copy(loading = false, items = list, error = null) }
            }
        }
    }

    private fun mapper(doc: com.google.firebase.firestore.DocumentSnapshot): CaseData {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        val label = doc.getString("label") ?: "Scan"
        val status = doc.getString("status") ?: "completed"
        val result = (doc.getString("result")
            ?: doc.getString("prediction")?.lowercase()
            ?: "unknown")
        val url = doc.getString("scan_url")
        val heatmapUrl = doc.getString("heatmap_url")
        val tsMs = doc.getLong("timestamp_ms")
            ?: doc.getTimestamp("timestamp")?.toDate()?.time
            ?: 0L
        val date = if (tsMs > 0) sdf.format(tsMs) else "—"

        return CaseData(
            caseId = doc.id,
            label = label,
            result = result,      // "benign"/"malignant"/"unknown"
            date = date,
            createdAt = tsMs,     // used for sorting in your screens
            status = status,      // "pending"/"completed"
            imageUrl = url,
            heatmapUrl = heatmapUrl
        )
    }

    private fun casesFlow(wherePending: Boolean): Flow<List<CaseData>> = callbackFlow {
        var q: Query = db.collection("lesion_case")
        if (wherePending) q = q.whereEqualTo("status", "pending")
        q = q.orderBy("timestamp", Query.Direction.DESCENDING)

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snap?.documents?.map(::mapper).orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }
}
