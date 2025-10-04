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
                    val status = doc.getString("status") ?: "completed"
                    val prediction = doc.getString("prediction") ?: "Pending"
                    val url = doc.getString("scan_url")
                    val date = doc.getTimestamp("timestamp")
                        ?.toDate()
                        ?.let { sdf.format(it) } ?: "—"

                    CaseData(
                        caseId = doc.id,
                        label = label,
                        result = prediction,
                        date = date,
                        status = status,
                        imageUrl = url
                    )
                }.orEmpty()

                trySend(items).isSuccess
            }

        awaitClose { registration.remove() }
    }
}