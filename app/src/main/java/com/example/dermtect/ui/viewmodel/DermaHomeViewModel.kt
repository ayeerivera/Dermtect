package com.example.dermtect.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.firestore.ListenerRegistration

// 🔑 NEW IMPORTS FOR COROUTINES, FIRESTORE QUERY, AND DATE FORMATTING
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ℹ️ Helper Data Classes (Assuming these are needed for internal ViewModel scope)
// NOTE: CaseData should ideally be defined in a common file, e.g., ui.components
data class DermaCaseData(
    val caseId: String,
    val label: String,
    val result: String?,
    val date: String,
    val status: String?,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val createdAt: Long = 0,
    val heatmapUrl: String? = null,
    val reportCode: String? = null
)

data class IndexedCase(
    val case: DermaCaseData,
    val scanNumber: Int
)

data class DermaReport(
    val id: String = "",
    val scanTitle: String = "Scan",
    val assessmentResult: String = "—",
    val notes: String = "—",
    val imageUrl: String = ""
)

class DermaHomeViewModel : ViewModel() {
    // --- Profile bits ---
    private val _firstName = mutableStateOf("")
    val firstName: State<String> = _firstName

    private val _lastName = mutableStateOf("")
    val lastName: State<String> = _lastName

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _isGoogleAccount = mutableStateOf(false)
    val isGoogleAccount: State<Boolean> = _isGoogleAccount

    private val _photoUri = mutableStateOf<Uri?>(null)
    val photoUri: State<Uri?> = _photoUri

    // --- Firestore / Auth handles ---
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    // --- Live listeners for counters ---
    private var regCases: ListenerRegistration? = null

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    init {
        android.util.Log.d("DermaVM", "uid=$uid")
        fetchUserBits()
        loadProfilePhoto()
        startCaseCounters()
    }

    private fun loadProfilePhoto() {
        val auth = FirebaseAuth.getInstance()
        val u = auth.currentUser ?: return
        u.photoUrl?.let { _photoUri.value = it }
        db.collection("users").document(u.uid)
            .get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("photoUrl")
                if (!url.isNullOrBlank()) _photoUri.value = Uri.parse(url)
            }
    }
    private fun fetchUserBits() {
        val u = uid ?: return
        db.collection("users").document(u)
            .get()
            .addOnSuccessListener { doc ->
                _firstName.value = doc.getString("firstName") ?: ""
                _lastName.value  = doc.getString("lastName") ?: ""
                _email.value     = doc.getString("email") ?: ""
                _isGoogleAccount.value = doc.getBoolean("isGoogleAccount") ?: false
            }
    }


    private fun startCaseCounters() {
        val u = uid ?: run {
            android.util.Log.w("DermaVM", "No UID; not listening for cases.")
            return
        }

        regCases?.remove()

        regCases = db.collection("lesion_case")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    android.util.Log.e("DermaVM", "cases listen error", e)
                    return@addSnapshotListener
                }

                val docs = snap?.documents ?: emptyList()

                val mine = docs.filter { d ->
                    val assessorId  = d.getString("assessor_id") ?: d.getString("assessorId")
                    val isAssessor = assessorId == u

                    val assessedByUid = d.getString("assessed_by_uid")
                    val isAssessedBy = assessedByUid == u

                    val openedBy = (d.get("opened_by") as? List<*>)?.map { it?.toString() }?.filterNotNull() ?: emptyList()
                    val participants = (d.get("participants") as? List<*>)?.map { it?.toString() }?.filterNotNull() ?: emptyList()

                    val isOpenedBy = openedBy.contains(u)
                    val isParticipant = participants.contains(u)

                    isAssessor || isOpenedBy || isParticipant || isAssessedBy
                }

                val total   = mine.size

                val pending = mine.count { (it.getString("derma_status") ?: "")
                    .equals("derma_pending", ignoreCase = true) }

                _totalCount.value   = total
                _pendingCount.value = pending

                android.util.Log.d(
                    "DermaVM",
                    "HOME COUNTS: total=$total pending=$pending (mine=${mine.size}, all=${docs.size})"
                )
            }
    }

    // 🔑 ADDED: toCaseData helper function
    private fun DocumentSnapshot.toDermaCaseData(): DermaCaseData {
        val assessedAtMs = getTimestamp("assessed_at")?.toDate()?.time
        val createdMs = assessedAtMs ?: getTimestamp("timestamp")?.toDate()?.time ?: 0L

        return DermaCaseData(
            caseId   = id,
            label    = getString("label") ?: "Scan",
            result   = getString("diagnosis"),
            date     = createdMs.formatAsDate(),
            status   = getString("derma_status"),
            imageUrl = getString("scan_url"),
            createdAt= createdMs,
            heatmapUrl = getString("heatmap_url")
        )
    }

    // 🔑 ADDED: formatAsDate extension function
    private fun Long.formatAsDate(): String {
        if (this <= 0L) return "—"
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(java.util.Date(this))
    }

    // 🔑 ADDED: The function to calculate the fixed scan number
    suspend fun getFixedScanNumber(caseId: String): Int? {
        val u = uid ?: return null // 1. Use the ViewModel's UID

        try {
            // A. Load ALL cases (needed to check all fields for 'mine')
            val allCasesQuery = db.collection("lesion_case")
                // Sort by the original timestamp for consistent chronological order
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val allDermaCaseData = allCasesQuery.documents
                .map { d -> d.toDermaCaseData() }

            // B. Filter the cases to include ONLY those relevant to the current Derma (u)
            val mineCases = allDermaCaseData.filter { dermacaseData ->
                val d = allCasesQuery.documents.first { it.id == dermacaseData.caseId } // Re-fetch DocumentSnapshot for list fields

                // Safely extract and check assessor ID
                val assessorId = d.getString("assessor_id") ?: d.getString("assessorId")
                val isAssessor = assessorId == u

                // Safely extract and check assessed_by_uid
                val assessedByUid = d.getString("assessed_by_uid")
                val isAssessedBy = assessedByUid == u

                // Safely extract and check list fields
                val openedBy = (d.get("opened_by") as? List<*>)?.map { it?.toString() }?.filterNotNull() ?: emptyList()
                val participants = (d.get("participants") as? List<*>)?.map { it?.toString() }?.filterNotNull() ?: emptyList()

                val isOpenedBy = openedBy.contains(u)
                val isParticipant = participants.contains(u)

                // CRITERIA: Case is "mine" if the derma is the assessor OR is listed as a participant/opened_by OR assessed by them
                isAssessor || isOpenedBy || isParticipant || isAssessedBy
            }

            // C. Assign permanent chronological number based on the filtered list (mineCases)
            val chronologicallyNumberedCases = mineCases
                .sortedBy { it.createdAt } // Ensure chronological sort
                .mapIndexed { index, dermacaseData ->
                    IndexedCase(case = dermacaseData, scanNumber = index + 1)
                }

            // D. Find the corresponding scan number from the new, smaller list
            return chronologicallyNumberedCases
                .firstOrNull { it.case.caseId == caseId }
                ?.scanNumber

        } catch (e: Exception) {
            android.util.Log.e("DermaVM", "Failed to get fixed scan number for $caseId", e)
            return null
        }
    }

    override fun onCleared() {
        regCases?.remove()
        super.onCleared()
    }
}