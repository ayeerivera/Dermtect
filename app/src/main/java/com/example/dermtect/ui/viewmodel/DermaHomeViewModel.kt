package com.example.dermtect.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.State


class DermaHomeViewModel : ViewModel() {
    private val _firstName = mutableStateOf("")
    val firstName: State<String> = _firstName

    init {
        fetchFirstName()
    }

    private fun fetchFirstName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                _firstName.value = doc.getString("firstName") ?: ""
            }
    }
}
