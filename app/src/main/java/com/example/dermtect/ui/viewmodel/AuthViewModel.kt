package com.example.dermtect.ui.viewmodel

import android.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dermtect.domain.usecase.AuthUseCase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val birthday: String = "",
    val email: String? = null
)
class AuthViewModel(private val authUseCase: AuthUseCase) : ViewModel() {

    // --- Firebase --- //
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // --- Loading / Result flags (legacy/back-compat) --- //
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    // ----------------------------------------------------
    // --- Persistent Auth State for app routing --- //
    sealed interface AuthUiState {
        data object Loading : AuthUiState
        data object SignedOut : AuthUiState
        data class EmailUnverified(val uid: String, val email: String?) : AuthUiState
        data class SignedIn(val uid: String, val emailVerified: Boolean) : AuthUiState
    }

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState


    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        if (user == null) {
            // No session -> SignedOut
            _authState.value = AuthUiState.SignedOut
        } else {
            // Have session -> evaluate role & verification from Firestore
            evaluateUserState(user)
        }
    }

    init {
        // Keep UI in Loading until listener fires the FIRST time.
        // This lets your Splash actually show while Firebase restores session.
        _authState.value = AuthUiState.Loading
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    // --- Audit logging --- //
    private fun logAudit(uid: String?, email: String?, action: String) {
        val logEntry = hashMapOf(
            "uid" to uid,
            "email" to email,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp()
        )
        Log.d("AuditLog", "Logging action: $action for uid=$uid email=$email")

        firestore.collection("audit_logs").add(logEntry)
            .addOnSuccessListener { Log.d("AuditLog", "Audit log saved.") }
            .addOnFailureListener { Log.e("AuditLog", "Failed to log audit event: ${it.message}") }
    }

    // --- Helper: Evaluate current user for routing (derma bypass) --- //
    // In AuthViewModel.kt
    private fun evaluateUserState(user: FirebaseUser) {
        // Step 1: Force the user object to reload data from Firebase server
        user.reload().addOnCompleteListener { reloadTask ->
            if (!reloadTask.isSuccessful) {
                Log.e("AuthViewModel", "Failed to reload user on auth state change: ${reloadTask.exception?.message}")
                // Fall through or handle error
            }

            // Step 2: Now that 'user' is refreshed, proceed with Firestore check
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: "patient"
                    val approved = doc.getBoolean("approved") ?: (role != "derma")

                    // ✅ This is now the FRESH verification status
                    val firebaseVerified = user.isEmailVerified

                    val allow = when (role) {
                        "derma" -> firebaseVerified && approved
                        else    -> firebaseVerified
                    }
                    val emailVerifiedField = doc.getBoolean("emailVerified") ?: false
                    if (user.isEmailVerified && !emailVerifiedField) {
                        doc.reference.update("emailVerified", true)
                            .addOnSuccessListener {
                                Log.d("AuthViewModel", "Firestore emailVerified field updated to true.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AuthViewModel", "Failed to update emailVerified in Firestore.", e)
                            }
                    }


                    _authState.value = if (allow) {
                        AuthUiState.SignedIn(user.uid, firebaseVerified)
                    } else {
                        AuthUiState.EmailUnverified(user.uid, user.email)
                    }
                }
                .addOnFailureListener {
                    // ... same fallback logic, but uses the freshly reloaded user ...
                    _authState.value = if (user.isEmailVerified) {
                        AuthUiState.SignedIn(user.uid, true)
                    } else {
                        AuthUiState.EmailUnverified(user.uid, user.email)
                    }
                }
        }
    }

    fun reloadAndRefresh() {
        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener { evaluateUserState(user) }
    }

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("AuthViewModel", "Cannot load profile: User not signed in.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    _userProfile.value = UserProfile(
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        birthday = doc.getString("birthday") ?: "",
                        email = doc.getString("email")
                    )
                } else {
                    Log.w("AuthViewModel", "User document not found for $uid")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.message}"
                Log.e("AuthViewModel", "Error fetching user profile", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    // --- Registration --- //
    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        birthday: String,
        role: String = "patient",            // NEW
        clinicName: String? = null,          // NEW
        contactNumber: String? = null,       // NEW
        clinicAddress: String? = null,
        credentials: String? = null // NEW
    ) {
        _loading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _loading.value = false

                    val ex = task.exception
                    _errorMessage.value = when (ex) {
                        is FirebaseAuthUserCollisionException ->
                            "This email is already registered. Please log in instead."
                        else ->
                            ex?.localizedMessage ?: "Registration failed. Please try again."
                    }

                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    _loading.value = false
                    _errorMessage.value = "Registration failed: no user."
                    return@addOnCompleteListener
                }

                val formattedFirstName = firstName.lowercase().replaceFirstChar { it.uppercaseChar() }
                val formattedLastName  = lastName.lowercase().replaceFirstChar { it.uppercaseChar() }

                val base = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "firstName" to formattedFirstName,
                    "lastName" to formattedLastName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "provider" to "email",
                    "role" to role
                )

                val userData = if (role == "derma") {
                    base + mapOf(
                        "birthday" to "",
                        "emailVerified" to false,
                        "approved" to false,
                        "credentials" to (credentials ?: ""),   // 👈 root-level for easy access
                        "derma" to mapOf(
                            "clinicName" to (clinicName ?: ""),
                            "contactNumber" to (contactNumber ?: ""),
                            "clinicAddress" to (clinicAddress ?: ""),
                            "credentials" to (credentials ?: "")
                        )
                    )
                } else {
                    base + mapOf(
                        "birthday" to birthday,
                        "emailVerified" to false,
                        "approved" to true              // 👈 patients are auto-approved
                    )
                }


                firestore.collection("users").document(user.uid).set(userData)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnCompleteListener { emailTask ->
                                _loading.value = false
                                if (emailTask.isSuccessful) {
                                    logAudit(user.uid, user.email, "account_created")
                                    FirebaseAuth.getInstance().signOut()
                                    _authSuccess.value = true
                                } else {
                                    _errorMessage.value = "Failed to send verification email."
                                }
                            }
                    }
                    .addOnFailureListener {
                        _loading.value = false
                        _errorMessage.value = "Failed to save user info."
                    }
            }
    }

    fun isPasswordStrong(password: String): Boolean {
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")
        return passwordRegex.matches(password)
    }

    // --- Email/Password Login --- //
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true

        authUseCase.loginUser(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (!task.isSuccessful) {
                    onError(task.exception?.message ?: "Login failed")
                    return@addOnCompleteListener
                }

                val user = auth.currentUser ?: return@addOnCompleteListener
                user.reload().addOnCompleteListener {
                    // Fetch role for derma bypass, update Firestore on verified email
                    firestore.collection("users").document(user.uid).get()
                        .addOnSuccessListener { document ->
                            val role = document.getString("role") ?: "patient"
                            val emailVerifiedField = document.getBoolean("emailVerified") ?: false
                            val approved = document.getBoolean("approved") ?: (role != "derma")

// Firebase is the REAL source of truth for verification
                            val firebaseVerified = user.isEmailVerified

// Who can log in?
                            val canLogin = when (role) {
                                // Dermatologists: must verify email AND be approved by admin
                                "derma" -> firebaseVerified && approved
                                // Patients: only need verified email
                                else    -> firebaseVerified
                            }

                            if (canLogin) {
                                // Mirror to Firestore: once Firebase says verified, mark field true
                                val updates = mutableMapOf<String, Any>(
                                    "provider" to "email"
                                )
                                if (!emailVerifiedField && firebaseVerified) {
                                    updates["emailVerified"] = true
                                }

                                document.reference.update(updates)
                                    .addOnSuccessListener {
                                        logAudit(user.uid, user.email, "login")
                                        _authSuccess.value = true
                                        onSuccess()
                                    }
                                    .addOnFailureListener {
                                        onError("Verified but failed to update Firestore.")
                                    }
                            } else {
                                // ✅ clearer error flow
                                val emailVerifiedNow = firebaseVerified

                                when {
                                    role == "derma" && !approved -> {
                                        val msg = "Your account is under review. Please wait for admin approval."
                                        _errorMessage.value = msg
                                        onError(msg)
                                    }

                                    !emailVerifiedNow -> {
                                        val msg = "Please verify your email first. Check your inbox or spam folder."
                                        _errorMessage.value = msg
                                        onError(msg)
                                    }

                                    else -> {
                                        val msg = "You cannot log in yet. Please contact support."
                                        _errorMessage.value = msg
                                        onError(msg)
                                    }
                                }
                            }

                        }
                        .addOnFailureListener {
                            onError("Failed to reload user info.")
                        }
                }
            }
    }

    // --- Verify helpers --- //
    fun resendVerificationEmail(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "No authenticated user.")
            return
        }
        user.sendEmailVerification()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message ?: "Failed to send email") }
    }

    // --- Logout --- //
    fun logout(onComplete: () -> Unit) {
        val user = auth.currentUser
        logAudit(user?.uid, user?.email, "logout")
        auth.signOut()
        onComplete()
        // Listener will emit SignedOut
    }

    // --- Account Deletion --- //
    fun deleteUser(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onFailure("No authenticated user.")
            return
        }
        val uid = user.uid
        val email = user.email

        // Optional: Firestore cleanup
        firestore.collection("users").document(uid).delete()

        // Audit log
        logAudit(uid, email, "Account Deleted")

        user.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Something went wrong")
            }
    }

    fun reauthenticateAndDelete(
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential)
                .addOnSuccessListener { deleteUser(onSuccess) { onFailure() } }
                .addOnFailureListener { onFailure() }
        } else {
            onFailure()
        }
    }

    // --- Error helpers --- //
    fun clearError() { _errorMessage.value = null }
    fun resetAuthSuccess() { _authSuccess.value = false }

    // --- Optional helpers --- //
    fun currentUserUid(): String? = auth.currentUser?.uid
}
