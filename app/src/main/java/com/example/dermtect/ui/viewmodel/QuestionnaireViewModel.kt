package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuestionnaireViewModel : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun saveQuestionnaireAnswers(
        answers: List<Boolean?>,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        _loading.value = true

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _loading.value = false
            onError("No user UID")   // <-- show clearly if this ever happens
            return
        }

        val db = FirebaseFirestore.getInstance()


        if (answers.any { it == null }) {
            onError("Not all questions answered"); return
        }

        val answerMap: Map<String, Boolean> = answers.mapIndexed { i, v ->
            "question_${i + 1}" to (v == true)
        }.toMap()

        val data = mapOf(
            "user_id" to uid,                           // <-- permanent canonical field
            "answers" to answerMap,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("questionnaires")
            .document(uid)                               // docId permanently = uid
            .set(data)
            .addOnSuccessListener { /* ... */ }
            .addOnFailureListener { e -> onError(e.message) }


        // 👇 Log where we write for sanity
        android.util.Log.d("QuestionnaireVM", "Writing to questionnaires/$uid")

        db.collection("questionnaires")
            .document(uid)
            .set(data)
            .addOnSuccessListener {
                _loading.value = false
                loadQuestionnaireAnswers()
                _saveSuccess.value = true
                logQuestionnaireAudit("questionnaire_updated")
                onSuccess()
            }
            .addOnFailureListener { e ->
                _loading.value = false
                android.util.Log.e("QuestionnaireVM", "Save failed", e)
                onError(e.message)   // <-- bubble the message up to the UI
            }

    }

    fun setExistingAnswers(newAnswers: List<Boolean?>) {
        _existingAnswers.value = newAnswers
    }


    fun resetSuccessFlag() {
        _saveSuccess.value = false
    }

    private val _existingAnswers = MutableStateFlow<List<Boolean?>?>(null)
    val existingAnswers: StateFlow<List<Boolean?>?> = _existingAnswers

    fun loadQuestionnaireAnswers() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("questionnaires")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document["answers"] as? Map<String, Boolean>
                    val loaded = (1..8).map { data?.get("question_$it") }
                    _existingAnswers.value = loaded
                }
            }
    }
    private fun logQuestionnaireAudit(action: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "unknown"
        val logData = mapOf(
            "uid" to uid,
            "email" to email,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("audit_logs")
            .add(logData)
    }

}
