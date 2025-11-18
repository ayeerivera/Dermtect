package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dermtect.ui.components.CaseData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp
data class HistoryUiState(
    val loading: Boolean = true,
    val items: List<CaseData> = emptyList(),
    val error: String? = null
)

class HistoryViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    val state = MutableStateFlow(HistoryUiState())

    init { observeCases() }

    private fun observeCases() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            state.update { it.copy(loading = false, error = "Not signed in") }
            return
        }

        viewModelScope.launch {
            casesFlow(uid).collect { list ->
                state.update { it.copy(loading = false, items = list, error = null) }
            }
        }
    }

    private fun casesFlow(uid: String): Flow<List<CaseData>> = callbackFlow {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        val registration = db.collection("lesion_case")
            .whereEqualTo("user_id", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                val items = snap?.documents?.map { doc ->
                    val label = doc.getString("label") ?: "Scan"

                    // Prefer assessed_at, else explicit ms, else created timestamp, else server createTime
                    val assessed   = doc.getTimestamp("assessed_at")?.toDate()
                    val created    = doc.getTimestamp("timestamp")?.toDate()
                    val tsMsField  = doc.getLong("timestamp_ms")

                    val createdTimestamp = doc.getTimestamp("timestamp")
                    val createdAtMs = when {
                        createdTimestamp != null -> createdTimestamp.toDate().time
                        doc.data?.containsKey("timestamp_ms") == true -> (doc.getLong("timestamp_ms") ?: 1L)
                        else -> 1L
                    }

                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(java.util.Date(createdAtMs))
                    // Keep your original status/prediction logic (unchanged)
                    val notes     = doc.getString("notes")
                    val diagnosis = doc.getString("diagnosis")
                    val prediction = doc.getString("diagnosis") ?: doc.getString("prediction") ?: "Pending"
                    val url = doc.getString("scan_url")
                    val date = doc.getTimestamp("timestamp")
                        ?.toDate()
                        ?.let { sdf.format(it) } ?: "—"

// 🟢 Show only "completed" or "reviewed"
                    val displayStatus = if (!notes.isNullOrBlank() || !diagnosis.isNullOrBlank())
                        "reviewed"
                    else
                        "completed"

                    val dermaStatus   = doc.getString("derma_status") // e.g., "pending", "completed", null
                    val generalStatus = doc.getString("status")       // e.g., "completed", "pending", null
                    val hasDermaNotes = !doc.getString("notes").isNullOrBlank() ||
                            !doc.getString("diagnosis").isNullOrBlank()

                    val uiStatus = when {
                        dermaStatus == "pending" -> "pending_review"
                        dermaStatus == "completed" && hasDermaNotes -> "reviewed"
                        generalStatus == "completed" -> "completed"
                        else -> null // no chip
                    }

                    CaseData(
                        caseId    = doc.id,
                        label     = label,
                        result    = doc.getString("diagnosis") ?: doc.getString("prediction") ?: "Pending",
                        date      = dateStr,
                        status = displayStatus, // ✅ changed line
                        imageUrl  = url,
                        createdAt = createdAtMs,
                        heatmapUrl= doc.getString("heatmap_url")
                    )

                }.orEmpty()


                trySend(items).isSuccess
            }

        awaitClose { registration.remove() }
    }
}