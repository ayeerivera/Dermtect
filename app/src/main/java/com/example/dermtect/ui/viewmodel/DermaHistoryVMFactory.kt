// com/example/dermtect/ui/viewmodel/DermaHistoryViewModelFactory.kt
package com.example.dermtect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore

class DermaHistoryVmFactory(
    private val feed: DermaFeed        // ✅ use the top-level enum directly
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DermaHistoryViewModel(
            db = FirebaseFirestore.getInstance(),
            feed = feed
        ) as T
    }
}
