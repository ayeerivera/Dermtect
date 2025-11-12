// com/example/dermtect/ui/viewmodel/DermaHistoryViewModel.kt
package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dermtect.ui.components.CaseData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

enum class StatusFilter { ALL, PENDING, COMPLETED }
enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }
enum class DermaFeed { PENDING_ONLY, ALL_CASES }

data class DermaHistoryUiState(
    val loading: Boolean = true,
    val items: List<CaseData> = emptyList(),
    val error: String? = null
)

class DermaHistoryViewModel(
    private val db: FirebaseFirestore,
    private val feed: DermaFeed,
    initialStatus: StatusFilter = StatusFilter.ALL,
    initialSort: SortOrder = SortOrder.NEWEST_FIRST,
) : ViewModel() {

    val state = MutableStateFlow(DermaHistoryUiState())

    private val statusFilter = MutableStateFlow(
        if (feed == DermaFeed.PENDING_ONLY) StatusFilter.PENDING else initialStatus
    )
    private val sortOrder = MutableStateFlow(initialSort)

    init {
        viewModelScope.launch {
            combine(statusFilter, sortOrder) { s, o -> s to o }
                .flatMapLatest { (s, o) -> casesFlow(s, o) }
                .catch { e -> state.update { it.copy(loading = false, error = e.message) } }
                .collect { list ->
                    state.update { it.copy(loading = false, items = list, error = null) }
                }
        }
    }

    fun setStatusFilter(s: StatusFilter) { statusFilter.value = s }
    fun setSortOrder(o: SortOrder)       { sortOrder.value = o }

    private fun mapper(doc: com.google.firebase.firestore.DocumentSnapshot): CaseData {
        val tsMs = doc.getLong("timestamp_ms")
            ?: doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
        val dateTxt = if (tsMs > 0)
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(tsMs)
        else "—"

        return CaseData(
            caseId = doc.id,
            label = doc.getString("label") ?: "Scan",
            result = (doc.getString("result") ?: doc.getString("prediction") ?: "unknown"),
            date = dateTxt,
            createdAt = tsMs,
            status = doc.getString("status") ?: "completed",
            imageUrl = doc.getString("scan_url"),
            heatmapUrl = doc.getString("heatmap_url")
        )
    }

    private fun casesFlow(
        status: StatusFilter,
        order: SortOrder
    ): Flow<List<CaseData>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var q: Query = db.collection("lesion_case")
            .where(
                Filter.or(
                    Filter.arrayContains("opened_by", uid),
                    Filter.equalTo("assessor_id", uid)
                )
            )

        // optional status narrowing
        q = when (status) {
            StatusFilter.PENDING   -> q.whereEqualTo("status", "pending")
            StatusFilter.COMPLETED -> q.whereEqualTo("status", "completed")
            StatusFilter.ALL       -> q
        }

        // sort
        val dir = if (order == SortOrder.NEWEST_FIRST)
            Query.Direction.DESCENDING else Query.Direction.ASCENDING
        q = q.orderBy("timestamp_ms", dir) // make sure you have this field & index

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snap?.documents?.map(::mapper).orEmpty())
        }
        awaitClose { reg.remove() }
    }
}
