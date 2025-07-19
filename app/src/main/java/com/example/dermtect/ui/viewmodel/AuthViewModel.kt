package com.example.dermtect.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.dermtect.domain.usecase.AuthUseCase
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(private val authUseCase: AuthUseCase) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage



    private fun logAudit(uid: String?, email: String?, action: String) {
        val db = FirebaseFirestore.getInstance()
        val logEntry = hashMapOf(
            "uid" to uid,
            "email" to email,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp()
        )

        Log.d("AuditLog", "Logging action: $action for uid=$uid email=$email")

        db.collection("audit_logs").add(logEntry)
            .addOnSuccessListener {
                Log.d("AuditLog", "Audit log saved.")
            }
            .addOnFailureListener {
                Log.e("AuditLog", "Failed to log audit event: ${it.message}")
            }
    }

    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome: StateFlow<Boolean> = _navigateToHome


    fun register(email: String, password: String, firstName: String, lastName: String) {
        _loading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    val formattedFirstName = firstName.lowercase().replaceFirstChar { it.uppercaseChar().toString() }
                    val formattedLastName = lastName.lowercase().replaceFirstChar { it.uppercaseChar().toString() }

                    val userData = hashMapOf(
                        "uid" to user?.uid,
                        "email" to user?.email,
                        "firstName" to formattedFirstName,
                        "lastName" to formattedLastName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "emailVerified" to false,
                        "role" to "patient",
                        "provider" to "email" // ✅ Add this line
                    )


                    user?.let {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                user.sendEmailVerification()
                                    .addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            logAudit(user.uid, user.email, "account_created")
                                            FirebaseAuth.getInstance().signOut()
                                            _authSuccess.value = true
                                        } else {
                                            _errorMessage.value = "Failed to send verification email."
                                        }
                                        _loading.value = false
                                    }
                            }
                            .addOnFailureListener {
                                _loading.value = false
                                _errorMessage.value = "Failed to save user info."
                            }
                    } ?: run {
                        _loading.value = false
                        _errorMessage.value = "Registration failed: no user."
                    }
                } else {
                    _loading.value = false
                    _errorMessage.value = task.exception?.message ?: "Registration failed"
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true

        authUseCase.loginUser(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.reload()?.addOnCompleteListener { reloadTask ->
                        if (reloadTask.isSuccessful) {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    val role = document.getString("role") ?: "user"
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
                                            _navigateToHome.value = true
                                            onSuccess()
                                        }.addOnFailureListener {
                                            onError("Verified but failed to update Firestore.")
                                        }
                                    } else {
                                        onError("Please verify your email first.")
                                        auth.signOut()
                                    }
                                }
                                .addOnFailureListener {
                                    onError("Failed to reload user info.")
                                }
                        }
                        }
                } else {
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val uid = user.uid
                    val email = user.email ?: ""

                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)

                    userDocRef.get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // If no document, create one
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
                                    }
                                    .addOnFailureListener { onError("Failed to save user data") }
                            } else {
                                logAudit(uid, email, "google_sign_in_existing")
                                onSuccess()
                            }
                        }
                        .addOnFailureListener {
                            onError("Failed to fetch user document")
                        }

                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Email already exists: try to link Google to the existing email/password account
                        val email = exception.email
                        if (email != null) {
                            onError("This email is already registered with another method. Please login with email/password first.")
                        } else {
                            onError("Email already in use. Try a different method.")
                        }
                    } else {
                        onError(exception?.localizedMessage ?: "Google Sign-In Failed")
                    }
                }
            }
    }

    fun markNavigationDone() {
        _navigateToHome.value = false
    }
    fun logout(onComplete: () -> Unit) {
        val user = auth.currentUser
        logAudit(user?.uid, user?.email, "logout")
        auth.signOut()
        onComplete()
    }

    fun deleteUser(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val email = user.email

            // Optional: Firestore cleanup
            firestore.collection("users").document(uid).delete()

            // Optional: Audit log
            logAudit(uid, email, "Account Deleted")

            user.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception ->
                    onFailure(exception.message ?: "Something went wrong")
                }
        } else {
            onFailure("No authenticated user.")
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
                .addOnSuccessListener {
                    // Use your existing deleteUser function
                    deleteUser(onSuccess, onFailure = { onFailure() })
                }
                .addOnFailureListener {
                    onFailure()
                }
        } else {
            onFailure()
        }
    }




    fun clearError() {
        _errorMessage.value = null
    }

    fun resetAuthSuccess() {
        _authSuccess.value = false
    }

}



