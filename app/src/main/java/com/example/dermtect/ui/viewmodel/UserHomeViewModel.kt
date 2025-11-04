package com.example.dermtect.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.dermtect.model.NewsItem
import androidx.lifecycle.AndroidViewModel
import com.example.dermtect.model.Clinic
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid = auth.currentUser?.uid.orEmpty()

    private val _firstName = MutableStateFlow("User")
    val firstName: StateFlow<String> = _firstName

    private val _hasConsented = MutableStateFlow(false)
    val hasConsented: StateFlow<Boolean> = _hasConsented

    private val _consentChecked = MutableStateFlow(false)
    val consentChecked: StateFlow<Boolean> = _consentChecked

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName

    private val _isGoogleAccount = MutableStateFlow(false)
    val isGoogleAccount: StateFlow<Boolean> = _isGoogleAccount

    private val _role = MutableStateFlow<String?>(null)
    val role: StateFlow<String?> = _role

    private val _tutorialSeen = MutableStateFlow<Boolean?>(null) // null = loading
    val tutorialSeen = _tutorialSeen.asStateFlow()
    fun fetchUserInfo() {
        if (uid.isEmpty()) return
        viewModelScope.launch {
            val userDoc = db.collection("users").document(uid).get().await()
            val first = userDoc.getString("firstName")?.replaceFirstChar { it.titlecase() } ?: "User"
            val last = userDoc.getString("lastName")?.replaceFirstChar { it.titlecase() } ?: ""
            val email = userDoc.getString("email") ?: ""
            val providers = FirebaseAuth.getInstance().currentUser?.providerData?.map { it.providerId }
            val isGoogle = providers?.contains("google.com") == true
            _role.value = userDoc.getString("role")


            _firstName.value = first
            _lastName.value = last
            _email.value = email
            _isGoogleAccount.value = isGoogle
        }
    }


    fun checkConsentStatus() {
        if (uid.isEmpty()) return
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            val consent = document.getBoolean("privacyConsent") ?: false
            _hasConsented.value = consent
            _consentChecked.value = true
        }
    }

    fun saveUserConsent() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("privacyConsent", true)
                    .await()

                _hasConsented.value = true
            } catch (_: Exception) {
                // Handle error if needed
            }
        }
    }

    suspend fun hasAnsweredQuestionnaire(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val document = FirebaseFirestore.getInstance()
            .collection("questionnaires")
            .document(uid)
            .get()
            .await()

        return document.exists()
    }
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews

    private val _highlightItem = MutableStateFlow<NewsItem?>(null)
    val highlightItem: StateFlow<NewsItem?> = _highlightItem

    fun fetchNews() {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val result = db.collection("news").get().await()
                val items = result.map { doc ->
                    val imageName = doc.getString("imageRes") ?: ""
                    val imageResId = if (imageName.isNotBlank()) {
                        context.resources.getIdentifier(imageName, "drawable", context.packageName)
                            .takeIf { it != 0 }
                    } else null

                    val imageUrl = doc.getString("imageUrl") // ✅ Fetch image URL from Firestore

                    NewsItem(
                        imageResId = imageResId,
                        imageUrl = imageUrl,                  // ✅ Add it here
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        body = doc.getString("body") ?: "",
                        source = doc.getString("source") ?: "",
                        date = doc.getString("date") ?: "",
                        isHighlight = doc.getBoolean("isHighlight") ?: false
                    )
                }


                // Split highlight vs normal
                _highlightItem.value = items.find { it.isHighlight }
                _newsItems.value = items.filter { !it.isHighlight }
            } catch (_: Exception) {
                _highlightItem.value = null
                _newsItems.value = emptyList()
            } finally {
                _isLoadingNews.value = false
            }
        }
    }
    private val _clinics = MutableStateFlow<List<Clinic>>(emptyList())
    val clinics: StateFlow<List<Clinic>> = _clinics

    private val _savedClinicIds = MutableStateFlow<List<String>>(emptyList())
    val savedClinicIds: StateFlow<List<String>> = _savedClinicIds

    private val _clinicList = MutableStateFlow<List<Clinic>>(emptyList())
    val clinicList: StateFlow<List<Clinic>> = _clinicList

    private val _isLoadingClinics = MutableStateFlow(true)
    val isLoadingClinics: StateFlow<Boolean> = _isLoadingClinics

    init {
        fetchClinics()
        fetchSavedClinics()
    }

    fun fetchClinics() {
        viewModelScope.launch {
            _isLoadingClinics.value = true
            try {
                val result = db.collection("clinics").get().await()

                val clinics = result.map { doc ->
                    Clinic(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        description = doc.getString("description") ?: "",
                        address = doc.getString("address") ?: "",
                        contact = doc.getString("contact") ?: "",
                        schedule = doc.getString("schedule") ?: "",
                        mapImage = doc.getString("mapImage") ?: ""
                    )
                }.filter { it.name.isNotBlank() }
                Log.d("FirestoreFetch", "Clinics fetched: ${clinics.map { it.name }}")

                Log.d("FETCH_CLINICS", "Fetched ${clinics.size} clinics: $clinics")
                _clinicList.value = clinics

            } catch (e: Exception) {
                Log.e("FETCH_CLINICS", "Failed to fetch clinics: ${e.message}")
                _clinicList.value = emptyList()
            } finally {
                _isLoadingClinics.value = false
            }
        }
    }

    fun fetchSavedClinics() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val savedIds = document.get("savedClinics") as? List<String> ?: emptyList()
                Log.d("SavedClinics", "Fetched: $savedIds")
                _savedClinicIds.value = savedIds

            }

            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Failed to fetch savedClinics", e)
            }
    }

    fun toggleClinicSave(clinicId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val savedRef = db.collection("users").document(currentUserId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(savedRef)
            val savedList = snapshot.get("savedClinics") as? List<String> ?: emptyList()
            val newList = if (clinicId in savedList) {
                savedList - clinicId
            } else {
                savedList + clinicId
            }
            transaction.update(savedRef, "savedClinics", newList)
        }.addOnSuccessListener {
            fetchSavedClinics()
        }
    }
    fun updateName(firstName: String, lastName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                )
            )
            .addOnSuccessListener {
                _firstName.value = firstName
                _lastName.value = lastName
            }
    }
    suspend fun loadTutorialSeenRemote() {
        val uid = auth.currentUser?.uid ?: run { _tutorialSeen.value = true; return } // no user → don't show
        try {
            val snap = db.collection("users").document(uid).get().await()
            _tutorialSeen.value = snap.getBoolean("tutorial_v1_seen") ?: false
        } catch (_: Exception) {
            _tutorialSeen.value = false // fail open: still show tutorial once
        }
    }

    fun markTutorialSeenRemote() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("tutorial_v1_seen", true)
            .addOnSuccessListener { _tutorialSeen.value = true }
            .addOnFailureListener { /* optional: retry/backoff */ }
    }

}
