package com.example.dermtect.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val tag = "SharedProfileVM"

    suspend fun loadPhoto(collection: String = "users") {
        try {
            val uid = auth.currentUser?.uid ?: return
            val snap = firestore.collection(collection).document(uid).get().await()

            // Read plain stored url (no ts in Firestore)
            val stored = snap.getString("photoUrl")
                ?: auth.currentUser?.photoUrl?.toString()

            _selectedImageUri.value = stored?.let { Uri.parse(withCacheBuster(it)) }
        } catch (e: Exception) {
            // log if you want
        }
    }

    fun setImageUri(localUri: Uri?, collection: String = "users") {
        if (localUri == null) return
        _selectedImageUri.value = localUri
        viewModelScope.launch { persistToCloud(localUri, collection) }
    }
    private fun withCacheBuster(url: String): String {
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}ts=${System.currentTimeMillis()}"
    }

    private suspend fun persistToCloud(localUri: Uri, collection: String) {
        val uid = auth.currentUser?.uid ?: return
        val version = System.currentTimeMillis()
        val ref = storage.reference.child("profilePhotos/$uid/$version.jpg")
        ref.putFile(localUri).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        // Store CLEAN url in Firestore
        firestore.collection(collection).document(uid)
            .set(mapOf("photoUrl" to downloadUrl), SetOptions.merge())
            .await()

        // Show in UI with cache buster so Coil refreshes
        _selectedImageUri.value = Uri.parse(withCacheBuster(downloadUrl))
    }

    fun clearImageUri(collection: String = "users") {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                runCatching {
                    val snap = firestore.collection(collection).document(uid).get().await()
                    snap.getString("photoUrl")?.let { url ->
                        val maybePath = url.substringAfter("o/").substringBefore("?").replace("%2F", "/")
                        if (maybePath.isNotBlank()) storage.reference.child(maybePath).delete().await()
                    }
                }
                firestore.collection(collection).document(uid)
                    .set(mapOf("photoUrl" to FieldValue.delete()), SetOptions.merge())
                    .await()
                _selectedImageUri.value = null
                _status.value = "cleared"
            } catch (e: Exception) {
                Log.e(tag, "clearImageUri failed", e)
                _status.value = "clear_err:${e.message}"
            }
        }
    }
}
