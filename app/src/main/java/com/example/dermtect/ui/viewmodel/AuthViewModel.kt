package com.example.dermtect.ui.viewmodel

import android.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome: StateFlow<Boolean> = _navigateToHome

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

    // We rely on the Firebase listener (which fires once upon add). Do NOT call it manually.
    private var gotFirstCallback = false

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        gotFirstCallback = true
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
    private fun evaluateUserState(user: FirebaseUser) {
        // Avoid unnecessary flicker back to Loading if we already know the user is logged in.
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "patient"
                val allow = (role == "derma") || user.isEmailVerified
                _authState.value = if (allow) {
                    AuthUiState.SignedIn(user.uid, user.isEmailVerified)
                } else {
                    AuthUiState.EmailUnverified(user.uid, user.email)
                }
            }
            .addOnFailureListener {
                // If role fetch fails, fall back to plain email verification
                _authState.value = if (user.isEmailVerified) {
                    AuthUiState.SignedIn(user.uid, true)
                } else {
                    AuthUiState.EmailUnverified(user.uid, user.email)
                }
            }
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
    ) {
        _loading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _loading.value = false
                    _errorMessage.value = task.exception?.message ?: "Registration failed"
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    _loading.value = false
                    _errorMessage.value = "Registration failed: no user."
                    return@addOnCompleteListener
                }

                val formattedFirstName = firstName.lowercase().replaceFirstChar { it.uppercaseChar() }
                val formattedLastName = lastName.lowercase().replaceFirstChar { it.uppercaseChar() }

                val userData = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "firstName" to formattedFirstName,
                    "lastName" to formattedLastName,
                    "birthday" to birthday,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "emailVerified" to false,
                    "role" to "patient",
                    "provider" to "email"
                )

                firestore.collection("users").document(user.uid).set(userData)
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnCompleteListener { emailTask ->
                                _loading.value = false
                                if (emailTask.isSuccessful) {
                                    logAudit(user.uid, user.email, "account_created")
                                    // Optional: sign out after registration to force email verification flow
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
                            val isVerified = user.isEmailVerified || role == "derma"

                            if (isVerified) {
                                document.reference.update(
                                    mapOf(
                                        "emailVerified" to true,
                                        "provider" to "email"
                                    )
                                ).addOnSuccessListener {
                                    logAudit(user.uid, user.email, "login")
                                    _authSuccess.value = true
                                    _navigateToHome.value = true // legacy signal
                                    onSuccess()
                                    // Listener will emit SignedIn after this anyway
                                }.addOnFailureListener {
                                    onError("Verified but failed to update Firestore.")
                                }
                            } else {
                                // Keep session; UI can show a Verify screen if you have one.
                                onError("Please verify your email first.")
                                _authState.value = AuthUiState.EmailUnverified(user.uid, user.email)
                            }
                        }
                        .addOnFailureListener {
                            onError("Failed to reload user info.")
                        }
                }
            }
    }

    // --- Google Sign-In --- //
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        val email = exception.email
                        if (email != null) {
                            onError("This email is already registered with another method. Please login with email/password first.")
                        } else {
                            onError("Email already in use. Try a different method.")
                        }
                    } else {
                        onError(exception?.localizedMessage ?: "Google Sign-In Failed")
                    }
                    return@addOnCompleteListener
                }

                val user = auth.currentUser ?: return@addOnCompleteListener
                val uid = user.uid
                val email = user.email ?: ""

                val userDocRef = firestore.collection("users").document(uid)
                userDocRef.get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            val names = user.displayName?.split(" ") ?: listOf("", "")
                            val firstName = names.getOrNull(0) ?: ""
                            val lastName = names.getOrNull(1) ?: ""

                            val userData = mapOf(
                                "uid" to uid,
                                "email" to email,
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "role" to "patient",
                                "provider" to "google",
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            userDocRef.set(userData)
                                .addOnSuccessListener {
                                    logAudit(uid, email, "google_sign_in_created")
                                    onSuccess()
                                    evaluateUserState(user)
                                }
                                .addOnFailureListener { onError("Failed to save user data") }
                        } else {
                            logAudit(uid, email, "google_sign_in_existing")
                            onSuccess()
                            evaluateUserState(user)
                        }
                    }
                    .addOnFailureListener { onError("Failed to fetch user document") }
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

    fun reloadAndRefresh() {
        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener { evaluateUserState(user) }
    }

    // --- Navigation helpers (legacy) --- //
    fun markNavigationDone() {
        _navigateToHome.value = false
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
