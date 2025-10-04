package com.example.dermtect.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ScanRepository {
    suspend fun reserveScanLabelAndId(
        db: FirebaseFirestore,
        uid: String
    ): Pair<String, String> {
        val userRef = db.collection("users").document(uid)
        val cases = db.collection("lesion_case")
        return db.runTransaction { tx ->
            val snap = tx.get(userRef)
            val next = (snap.getLong("nextScanIndex") ?: 1L).toInt()
            if (!snap.exists()) {
                tx.set(userRef, mapOf("nextScanIndex" to next + 1))
            } else {
                tx.update(userRef, mapOf("nextScanIndex" to next + 1))
            }
            val caseId = cases.document().id
            caseId to "Scan $next"
        }.await()
    }
}