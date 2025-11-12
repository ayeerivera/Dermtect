package com.example.dermtect.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.State
import com.example.dermtect.ui.components.DermaReport
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

data class DermaReport(
    val id: String = "",
    val scanTitle: String = "Scan",
    val assessmentResult: String = "—",
    val notes: String = "—",
    val imageUrl: String = ""
)

class DermaHomeViewModel : ViewModel() {
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

    init {
        loadProfilePhoto()
    }
    private fun loadProfilePhoto() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        // If Google account has a photo:
        auth.currentUser?.photoUrl?.let {
            _photoUri.value = it
        }

        // Firestore "photoUrl" (must be HTTPS, not gs://)
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("photoUrl") // store an https download URL here
                if (!url.isNullOrBlank()) {
                    _photoUri.value = Uri.parse(url)
                }
            }
    }

    // If you saved a Firebase Storage PATH instead of https URL, use this:
    fun resolveFromStoragePath(pathOrGsUrl: String) {
        val ref = if (pathOrGsUrl.startsWith("gs://"))
            Firebase.storage.getReferenceFromUrl(pathOrGsUrl)
        else
            Firebase.storage.getReference(pathOrGsUrl)

        ref.downloadUrl.addOnSuccessListener { uri ->
            _photoUri.value = uri
        }
    }
    init { fetchUserBits() }

    private fun fetchUserBits() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                _firstName.value = doc.getString("firstName") ?: ""
                _lastName.value  = doc.getString("lastName") ?: ""
                _email.value     = doc.getString("email") ?: ""
                _isGoogleAccount.value = doc.getBoolean("isGoogleAccount") ?: false
            }
    }

    fun updateName(first: String, last: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(mapOf("firstName" to first, "lastName" to last))
    }
}

class RetrieveCaseViewModel : ViewModel() {
    var showInputDialog = mutableStateOf(false)
    var reportId = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var loadedReport = mutableStateOf<DermaReport?>(null)
    var showReportDialog = mutableStateOf(false)

    fun open() { showInputDialog.value = true }
    fun close() {
        showInputDialog.value = false
        isLoading.value = false
        error.value = null
        reportId.value = ""
    }

    fun closeReport() {
        showReportDialog.value = false
        loadedReport.value = null
    }

    fun setError(msg: String?) { error.value = msg }
}
