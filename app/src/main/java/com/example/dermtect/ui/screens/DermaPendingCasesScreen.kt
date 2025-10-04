// com/example/dermtect/ui/screens/PendingCasesScreen.kt
package com.example.dermtect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.dermtect.ui.components.CaseData
import com.example.dermtect.ui.components.HistoryScreenTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PendingCasesScreen(navController: NavController) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<CaseData>>(emptyList()) }

    // Listen to Firestore changes while this screen is on
    DisposableEffect(uid) {
        if (uid == null) {
            loading = false
            error = "Not signed in"
            onDispose { }
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val reg = db.collection("lesion_case")
                .whereEqualTo("user_id", uid)
                .whereEqualTo("status", "pending") // only pending
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        error = err.message
                        items = emptyList()
                        loading = false
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.map { doc ->
                        CaseData(
                            caseId = doc.id,
                            label = doc.getString("label") ?: "Scan",
                            result = doc.getString("prediction") ?: "Pending",
                            date = doc.getTimestamp("timestamp")
                                ?.toDate()
                                ?.let { sdf.format(it) } ?: "—",
                            status = doc.getString("status") ?: "pending",
                            imageUrl = doc.getString("scan_url"),
                            imageRes = null
                        )
                    }.orEmpty()
                    items = list
                    loading = false
                    error = null
                }
            onDispose { reg.remove() }
        }
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            // Simple fallback; you could also show HistoryScreenTemplate with empty list
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $error")
            }
        }
        else -> {
            HistoryScreenTemplate(
                navController = navController,
                screenTitle = "Pending Cases",
                caseList = items
            )
        }
    }
}