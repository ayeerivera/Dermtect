package com.example.dermtect.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Source
import androidx.compose.material.icons.Icons
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.material3.Card
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.outlined.ChevronRight // <-- FIX: If using outlined icons
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
@Composable
fun DermaAssessmentScreenReport(
    caseId: String,
    startInEditMode: Boolean = false,   // 👈 NEW
    onBackClick: () -> Unit

) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var originalStatus by remember { mutableStateOf<String?>(null) }
    var originalDiagnosis by remember { mutableStateOf<String?>(null) }
    var originalNotes by remember { mutableStateOf("") }

    var scanTitle by remember { mutableStateOf("Scan") }
    var lesionImageUrl by remember { mutableStateOf<String?>(null) }
    var diagnosis by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var realCaseId by remember { mutableStateOf<String?>(null) }
    val canSave by remember { derivedStateOf { !diagnosis.isNullOrBlank() } }
    var showCancelDialog by remember { mutableStateOf(false) }

    var assessedScanNo by remember { mutableStateOf<Long?>(null) }
    var assessedAt by remember { mutableStateOf<com.google.firebase.Timestamp?>(null) }
    var showSaved by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }     // "pending" / "completed"
    var editMode by remember { mutableStateOf(startInEditMode) }
    val readOnly by remember { derivedStateOf { !editMode } }    // simpler: readOnly = !editMode

    var showRiskSheet by remember { mutableStateOf(false) }
    var reportCode by remember { mutableStateOf<String?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }

    // Inside DermaAssessmentScreenReport:

    val scrollState = rememberScrollState()


    var modelPrediction by remember { mutableStateOf<String?>(null) }

// 🔑 NEW: Visibility flag for the bottom sheet
    var showModelSummarySheet by remember { mutableStateOf(false) }

// 🔑 NEW: Detailed Model Output States
    var modelDetailedSummary by remember { mutableStateOf<String?>(null) }
    var rawProbability by remember { mutableStateOf<Double?>(null) }
// Inside DermaAssessmentScreenReport:

// ...
    // Load case + existing assessment
    LaunchedEffect(caseId) {
        loading = true
        error = null
        try {
            val cases = db.collection("lesion_case")

            val byId = cases.document(caseId).get(Source.SERVER).await()
            val finalDoc = if (byId.exists()) {
                realCaseId = caseId
                byId
            } else if (caseId.length == 4) {
                val code = caseId.trim().uppercase()
                val snap = cases.whereEqualTo("report_code", code).limit(1).get(Source.SERVER).await()
                if (!snap.isEmpty) {
                    realCaseId = snap.documents.first().id
                    cases.document(realCaseId!!).get(Source.SERVER).await()
                } else null
            } else null

            if (finalDoc == null || !finalDoc.exists()) {
                error = "Case not found for '$caseId'."
                return@LaunchedEffect
            }

            val patientUid = finalDoc.getString("user_id")
            val dermaUid = FirebaseAuth.getInstance().currentUser?.uid

            if (!patientUid.isNullOrBlank() && !dermaUid.isNullOrBlank()) {
                try {
                    db.collection("questionnaires")
                        .document(patientUid)
                        .set(
                            mapOf("shared_with_dermas" to FieldValue.arrayUnion(dermaUid)),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e("ShareQuestionnaire", "Failed to share questionnaire: ${e.message}")
                }
            }

            // parent fields
            // parent fields
// 🔍 Extra safety: also read from derma_assessment/latest if present
            try {
                val latestSnap = db.collection("lesion_case")
                    .document(finalDoc.id)
                    .get(Source.SERVER)
                    .await()

                if (latestSnap.exists()) {
                    val latestDiagnosis = latestSnap.getString("diagnosis")
                    val latestNotes = latestSnap.getString("notes")

                    if (!latestDiagnosis.isNullOrBlank()) {
                        diagnosis = latestDiagnosis
                    }
                    if (!latestNotes.isNullOrBlank()) {
                        notes = latestNotes
                    }
                }
            } catch (_: Throwable) {
                // ignore, we already have parent fields
            }
            scanTitle      = finalDoc.getString("label") ?: "Scan"
            reportCode     = finalDoc.getString("report_code")
            lesionImageUrl = finalDoc.getString("scan_url")
            assessedScanNo = finalDoc.getLong("assessed_scan_no")
            assessedAt     = finalDoc.getTimestamp("assessed_at")
            diagnosis      = finalDoc.getString("diagnosis")
            notes          = finalDoc.getString("notes") ?: ""

            // 🔑 FIX 1: Read status from the consistent field "status", falling back to "assessment_status"
            status = finalDoc.getString("derma_status")



            // remember originals for cancel behavior
            originalStatus    = status
            originalDiagnosis = diagnosis
            originalNotes     = notes

            /// only change auto-mode IF not forced by search
            if (!startInEditMode) {
                editMode = !status.equals("completed", ignoreCase = true)
            }
// Modify to assign to the state variable:
            val rawPrediction = finalDoc.getString("prediction")
            rawProbability = finalDoc.getDouble("probability") // 🔑 FIX: Assign to state variable here

// 1. Calculate the high-level summary for the button
            modelPrediction = calculateModelSummary(rawPrediction, rawProbability)

// 2. Set the detailed summary RATIONALE
            modelDetailedSummary = getModelRationaleMessage(rawProbability) // Now uses the state variable

            val currentStatus = finalDoc.getString("status") ?: ""
            val currentAssessorId = finalDoc.getString("assessor_id")

            if (dermaUid != null && !currentStatus.equals("completed", ignoreCase = true)) {
                try {
                    // 🔑 FIX 2: Use "derma_pending" status string for consistency
                    val updates = mutableMapOf<String, Any>("derma_status" to "derma_pending")
                    if (currentAssessorId.isNullOrBlank()) {
                        updates["assessor_id"] = dermaUid   // 👈 unified field
                    }
                    if (finalDoc.get("timestamp") == null) {
                        updates["timestamp"] = FieldValue.serverTimestamp()
                    }

                    db.collection("lesion_case")
                        .document(finalDoc.id)
                        .update(updates)
                        .await()
                } catch (e: Throwable) {
                    error = "Failed to mark pending: ${e.message}"
                }
            }
        } catch (t: Throwable) {
            error = "Load failed: ${t.message}"
        } finally {
            loading = false
        }
    }



    BubblesBackground {
        val scanNoText = assessedScanNo?.let { "Scan #$it" } ?: "Scan"
        val dateText = assessedAt?.toDate()?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault()).format(it)
        } ?: "—"

        val reportCodeText= reportCode ?: "—"


        Box(Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 10.dp)
            ) {
                BackButton(
                    onClick = {
                        if (editMode) showCancelDialog = true else onBackClick()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 23.dp)
                )


                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = scanNoText, // ← shows "Scan #N" or "Scan"
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = dateText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Report ID: ${reportCodeText ?: "—"}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray)
                    )
                    Spacer(Modifier.height(10.dp))

                }


            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error", color = Color.Red)
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 160.dp, bottom = 50.dp, start = 20.dp, end = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Image
                        val imageSide = 280.dp // match user screen feel
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(imageSide)
                                .fillMaxWidth()
                                .clickable(enabled = !lesionImageUrl.isNullOrBlank()) {
                                    if (!lesionImageUrl.isNullOrBlank()) {
                                        showImageDialog = true
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                        ) {
                            if (!lesionImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = lesionImageUrl,
                                    contentDescription = "Lesion Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No image", color = Color.Gray)
                                }
                            }
                        }


                        Spacer(Modifier.height(16.dp))

// ✅ Additional Context button
                        // 🔑 NEW: Combined Model Prediction and Risk Card
                        if (!modelPrediction.isNullOrBlank()) {
                            ModelAndRiskCard(
                                summary = modelPrediction!!,
                                onClick = { showRiskSheet = true } // 🔑 Now opens the AdditionalContextSheet
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        val btnHeight = 56.dp
                        val benignSelected = diagnosis == "Benign"
                        val malignantSelected = diagnosis == "Malignant"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (benignSelected) {
                                PrimaryButton(
                                    text = "Benign",
                                    onClick = { diagnosis = "Benign" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true                                )
                            } else {
                                SecondaryButton(
                                    text = "Benign",
                                    onClick = { diagnosis = "Benign" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true                                )
                            }

                            if (malignantSelected) {
                                PrimaryButton(
                                    text = "Malignant",
                                    onClick = { diagnosis = "Malignant" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true                                )
                            } else {
                                SecondaryButton(
                                    text = "Malignant",
                                    onClick = { diagnosis = "Malignant" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true                                )
                            }
                        }


// --- Derma Notes (optional) ---
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Notes (optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(Modifier.height(8.dp))

                        val maxChars = 800
                        var notesError by remember { mutableStateOf<String?>(null) }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { new ->
                                if (editMode) {                     // ← was !readOnly
                                    if (new.length <= maxChars) {
                                        notes = new
                                        notesError = null
                                    } else notesError = "Max $maxChars characters"
                                }
                            },
                            enabled = editMode,
                            placeholder = {
                                Text(
                                    text = "Write your observations…",
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            minLines = 4,
                            maxLines = 8,
                            isError = notesError != null,
                            supportingText = {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (notesError != null)
                                        Text(notesError!!, color = MaterialTheme.colorScheme.error)
                                    Text("${notes.length}/$maxChars")
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrect = true
                            )
                        )


                        // Action buttons (vertically stacked)
                        // Footer actions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!editMode) {
                                // ✅ READ-ONLY (e.g., completed case from History)
                                PrimaryButton(
                                    text = "Edit",
                                    onClick = { editMode = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                )
                            } else {
                                // ✏️ EDITING (pending or editing a completed one)
                                PrimaryButton(
                                    text = if (saving) "Saving…" else "Save",
                                    onClick  = {
                                        if (!canSave || saving) return@PrimaryButton
                                        val id = realCaseId ?: run {
                                            error = "Missing case id."; return@PrimaryButton
                                        }
                                        val dermaId =
                                            FirebaseAuth.getInstance().currentUser?.uid ?: run {
                                                error = "Not signed in."; return@PrimaryButton
                                            }

                                        saving = true
                                        scope.launch {
                                            try {
                                                // Save as completed (even when editing a completed case)
                                                db.collection("lesion_case").document(id)
                                                    .update(
                                                        mapOf(
                                                            "assessor_id" to dermaId,   // 👈 here too
                                                            "assessed_at" to FieldValue.serverTimestamp(),
                                                            "diagnosis"   to (diagnosis ?: ""),
                                                            "notes"       to notes,
                                                            "derma_status"      to "completed"
                                                        )
                                                    )
                                                    .await()


                                                // Optional: append audit entry
                                                // --- update or create the latest subdocument ---
                                                val latestRef = db.collection("lesion_case")
                                                    .document(id)

                                                val entry = mapOf(
                                                    "assessor_id" to dermaId,   // 👈 keep consistent
                                                    "assessed_at" to FieldValue.serverTimestamp(),
                                                    "diagnosis"   to (diagnosis ?: ""),
                                                    "notes"       to notes,
                                                    "derma_status"      to "completed"
                                                )


// Write (merge ensures we keep existing fields if any)
                                                latestRef.set(entry, SetOptions.merge()).await()

// --- optionally still add a separate history entry ---


                                               status =
                                                    "completed"   // ← now we know a save happened here
                                                editMode =
                                                    false            // ← flip to READ for this session
                                                showSaved = true
                                            } catch (t: Throwable) {
                                                error = "Save failed: ${t.message}"
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    },
                                    enabled = canSave && !saving,
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )

                                SecondaryButton(
                                    text = "Cancel",
                                    onClick = {
                                        showCancelDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                )
                            }
                        }

                    }
                }
            }

            val scope = rememberCoroutineScope() // Ensure this is present

            val canScroll by remember {
                // True if there is content to scroll
                derivedStateOf { scrollState.maxValue > 0 }
            }

            val atBottom by remember {
                // 🔑 FIX: This checks if the user is within 8dp of the max scroll value
                derivedStateOf { scrollState.value >= scrollState.maxValue - 800 }
            }
// ...
            // In DermaAssessmentScreenReport, replace the AnimatedVisibility block:

// ▼▼ fixed bottom-center overlay (sibling of Column)
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomCenter)   // keep it pinned to bottom
                    // 🔑 FINAL FIX: Use a large, safe padding to lift the arrow well above the buttons
                    .padding(bottom = 60.dp),
                visible = canScroll && !atBottom,    // hide when already at bottom
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFBFFDFD),
                                    Color(0xFF88E7E7),
                                    Color(0xFF55BFBF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            scope.launch {
                                val target = scrollState.maxValue
                                scrollState.animateScrollTo(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Scroll down",
                        tint = Color(0xFF0FB2B2),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (showSaved) {
                DialogTemplate(
                    show = true,
                    title = "Saved!",
                    autoDismiss = true,
                    dismissDelay = 1000L,
                    onDismiss = {
                        showSaved = false
                        onBackClick()
                    }
                )
            }

            if (showRiskSheet) {
                AdditionalContextSheet(
                    caseId = realCaseId,
                    modelPrediction = modelPrediction,
                    modelDetailedSummary = modelDetailedSummary, // 🔑 FIX: Pass the already calculated state
                    onDismiss = { showRiskSheet = false }
                )
            }

            val isCompleted = originalStatus.equals("completed", ignoreCase = true)
            val cancelDescription = if (isCompleted) {
                "This will discard your unsaved changes. The previous completed assessment will remain."
            } else {
                "This will mark the case as Pending and discard your diagnosis and notes. Nothing will be saved."
            }


            if (showCancelDialog) {
                DialogTemplate(
                    show = true,
                    title = "Cancel assessment?",
                    description = cancelDescription,
                    primaryText = "Yes, cancel",
                    onPrimary = {
                        val id = realCaseId ?: return@DialogTemplate

                        // 🔁 Restore local UI to the original values
                        diagnosis = originalDiagnosis
                        notes = originalNotes
                        status = originalStatus
                        editMode = false
                        showCancelDialog = false

                        onBackClick()

                        // 🚫 IMPORTANT: Only write "pending" to Firestore
                        // if the case was NOT completed originally.
                        // FIX 1: This check now correctly uses the status loaded from the parent doc.
                        if (!originalStatus.equals("completed", ignoreCase = true)) {
                            scope.launch {
                                try {
                                    val db = FirebaseFirestore.getInstance()
                                    val dermaUid = FirebaseAuth.getInstance().currentUser?.uid

                                    val caseRef = db.collection("lesion_case").document(id)

                                    val batch = db.batch()

                                    // 1) Parent doc: set status = derma_pending
                                    batch.set(
                                        caseRef,
                                        mapOf(
                                            "derma_status" to "derma_pending", // 🔑 FIX 2: Use consistent "derma_pending"
                                            "assessor_id" to (dermaUid ?: ""),
                                            "assessed_at" to FieldValue.serverTimestamp()
                                        ),
                                        SetOptions.merge()
                                    )

                                    // 1.1) Optionally clear diagnosis/notes
                                    batch.update(
                                        caseRef,
                                        mapOf(
                                            "diagnosis" to FieldValue.delete(),
                                            "notes" to FieldValue.delete()
                                        )
                                    )



                                    batch.commit().await()
                                } catch (_: Throwable) {
                                    // optional: log / snackbar
                                }
                            }
                        }
                    },

                    secondaryText = "Keep editing",
                    onSecondary = { showCancelDialog = false
                        editMode = true},
                    onDismiss = { showCancelDialog = false }
                )
            }
            if (showImageDialog && !lesionImageUrl.isNullOrBlank()) {
                Dialog(onDismissRequest = { showImageDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        AsyncImage(
                            model = lesionImageUrl,
                            contentDescription = "Lesion Image Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }


            }

        }
    }
}





// model
data class DermaReport(
    val id: String = "",
    val assessedScanNo: Long? = null,
    val assessedAt: com.google.firebase.Timestamp? = null,
    val assessmentResult: String = "—",
    val notes: String = "—",
    val imageUrl: String = ""
)

data class QAResult(
    val answers: Map<String, Any>,
    val source: String
)

private suspend fun loadQuestionnaireFromTopLevel(
    db: FirebaseFirestore,
    caseId: String
): QAResult {
    // 1) get case → user_id
    val caseSnap = db.collection("lesion_case").document(caseId).get(Source.SERVER).await()
    val userId = caseSnap.getString("user_id") ?: return QAResult(emptyMap(), "none")

    // 2) fetch questionnaires/{user_id}
    val qSnap = db.collection("questionnaires").document(userId).get(Source.SERVER).await()
    if (!qSnap.exists()) return QAResult(emptyMap(), "questionnaires/$userId (missing)")

    // answers can either be nested under field "answers" (Map) or be top-level fields
    val root = qSnap.data.orEmpty()
    val nested = (root["answers"] as? Map<*, *>)?.filterKeys { it is String }?.mapValues { it.value ?: "—" }
        ?.mapKeys { it.key as String }
        ?: emptyMap()

    // prefer nested "answers" if present, otherwise fall back to top-level question_* fields
    val answers: Map<String, Any> =
        if (nested.isNotEmpty()) nested
        else root.filterKeys { k -> k.startsWith("question_") }

    return QAResult(answers, "questionnaires/$userId")
}


private fun List<*>.joinedPretty(): String =
    this.joinToString(separator = "\n") { "• ${it ?: "—"}" }

data class RiskAssessmentPayload(
    val answers: Map<String, Any>,
    val pdfUrl: String?
)

private suspend fun loadRiskAssessmentPayload(
    db: FirebaseFirestore,
    caseId: String
): RiskAssessmentPayload {
    // 0) Read parent case doc to get owner + parent pdf fallback
    val caseSnap = db.collection("lesion_case").document(caseId).get(Source.SERVER).await()
    val parentPdf = caseSnap.getString("report_pdf_url")
    val ownerUid = caseSnap.getString("user_id")

    // 1) Try fixed doc: lesion_case/{caseId}/risk_assessment/latest
    run {
        val latest = db.collection("lesion_case").document(caseId)
            .collection("risk_assessment")
            .document("latest")
            .get(Source.SERVER)
            .await()

        if (latest.exists()) {
            val answers = latest.data?.filterKeys { it != "report_pdf_url" } ?: emptyMap()
            val url = latest.getString("report_pdf_url") ?: parentPdf
            return RiskAssessmentPayload(answers, url)
        }
    }

    // 2) Try newest by timestamp field(s) in risk_assessment collection
    runCatching {
        val colRef = db.collection("lesion_case").document(caseId)
            .collection("risk_assessment")

        // Try a few common timestamp fields; first one that works wins
        val candidates = listOf("updatedAt", "createdAt", "timestamp")
        for (field in candidates) {
            val q = colRef.orderBy(field, com.google.firebase.firestore.Query.Direction.DESCENDING).limit(1)
            val qSnap = q.get(Source.SERVER).await()
            if (!qSnap.isEmpty) {
                val d = qSnap.documents.first()
                val answers = d.data?.filterKeys { it != "report_pdf_url" } ?: emptyMap()
                val url = d.getString("report_pdf_url") ?: parentPdf
                return RiskAssessmentPayload(answers, url)
            }
        }
    }

    // 3) Try user-scoped fallback (if your questionnaire saved here)
    //    users/{ownerUid}/risk_assessment/{caseId}  OR  users/{ownerUid}/questionnaire/{caseId}
    if (!ownerUid.isNullOrBlank()) {
        val userPaths = listOf(
            "risk_assessment", "questionnaire"
        )
        for (sub in userPaths) {
            val userDoc = db.collection("users").document(ownerUid)
                .collection(sub).document(caseId).get(Source.SERVER).await()
            if (userDoc.exists()) {
                val answers = userDoc.data?.filterKeys { it != "report_pdf_url" } ?: emptyMap()
                val url = userDoc.getString("report_pdf_url") ?: parentPdf
                return RiskAssessmentPayload(answers, url)
            }
        }
    }

    // 4) Fallback: nothing found, but we can still return parent PDF if present
    return RiskAssessmentPayload(emptyMap(), parentPdf)
}

// 🔑 NEW: Combined Model Prediction and Risk Card
@Composable
fun ModelAndRiskCard(
    summary: String, // modelPrediction: "BENIGN (Very Low Risk - Prob: 0.4%)"
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Extract the risk label from the summary string
    val riskLabel = summary
        .substringAfter("(")
        .substringBefore("Risk")
        .trim()

    // 2. Get dynamic colors
    val backgroundColor = getRiskColor(riskLabel)
    val borderColor = getBorderColor(riskLabel)
    val contentColor = Color.Black.copy(alpha = 0.8f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Info",
                tint = borderColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Dermtect Prediction:",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = summary,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "View Details",
                tint = borderColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalContextSheet(
    caseId: String?,
    // 🔑 NEW PARAMETERS:
    modelPrediction: String?,
    modelDetailedSummary: String?,
    onDismiss: () -> Unit
) {
    if (caseId == null) {
        onDismiss()
        return
    }

    val db = remember { FirebaseFirestore.getInstance() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    // NOTE: answers map keys are Strings ("question_1"), values are Booleans (true/false)
    var answers by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // Your 10 friendly questions
    val questionLabels = remember {
        listOf(
            "Do you usually get sunburned easily after spending around 15–20 minutes under the sun without protection?",
            "Is your natural skin color fair or very fair (light and easily burns in the sun)?",
            "Have you ever had a severe sunburn that caused redness or blisters and lasted for more than a day?",
            "Do you have many moles or freckles on your body (for example, more than 50 small spots or several large moles)?",
            "Has any of your close family members (parent, sibling, or child) been diagnosed with skin cancer?",
            "Have you ever been diagnosed or treated for any type of skin cancer or a precancerous skin lesion?",
            "Do you often spend more than one hour outdoors during peak sunlight (between 10 a.m. and 4 p.m.) without shade or protection?",
            "Do you rarely or never use sunscreen when you go outdoors for long periods?",
            "Do you seldom check your skin or moles for any new or changing spots?",
            "Have you recently noticed a new or changing mole or spot on your skin in the last six months?"
        )
    }


    LaunchedEffect(caseId) {
        loading = true
        error = null
        try {
            val caseSnap = db.collection("lesion_case")
                .document(caseId)
                .get(Source.SERVER)
                .await()

            val patientUid = caseSnap.getString("user_id")
                ?: throw IllegalStateException("Missing user_id on case")

            val dermaUid = FirebaseAuth.getInstance().currentUser?.uid
            if (!dermaUid.isNullOrBlank()) {
                try {
                    db.collection("questionnaires")
                        .document(patientUid)
                        .set(
                            mapOf("shared_with_dermas" to FieldValue.arrayUnion(dermaUid)),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e("ShareQuestionnaire", "Failed to share questionnaire: ${e.message}")
                }
            }

            val qSnap = db.collection("questionnaires")
                .document(patientUid)
                .get(Source.SERVER)
                .await()

            if (!qSnap.exists()) {
                answers = emptyMap()
            } else {
                val root = qSnap.data.orEmpty()
                val nested = root["answers"] as? Map<*, *>

                @Suppress("UNCHECKED_CAST")
                answers = if (nested != null) {
                    // Extract answers from nested map
                    nested.entries
                        .filter { (k, v) -> k is String && v is Boolean }
                        .associate { (k, v) -> k as String to v as Boolean }
                } else {
                    // Extract answers from root level
                    root
                        .filter { (k, v) -> k.startsWith("question_") && v is Boolean }
                        .mapValues { (_, v) -> v as Boolean }
                }
            }
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load questionnaire."
            answers = emptyMap()
        } finally {
            loading = false
        }
    }

    // Define scroll state outside the ModalBottomSheet for use in derivedStateOf
    val scrollState = rememberScrollState()
    val showScrollHint by remember {
        // Show hint if there's content to scroll and we're not near the bottom
        derivedStateOf { scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - 20 }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState) // <-- Scroll state applied here
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔑 NEW: MODEL RATIONALE SECTION
                if (!modelPrediction.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFCDFFFF).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Dermtect Prediction:",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0E4C4C)
                            )
                            Spacer(Modifier.height(8.dp))

                            // Primary Prediction/Risk (from button text)
                            Text(
                                text = modelPrediction!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF0E4C4C)
                            )

                            Divider(Modifier.padding(vertical = 12.dp))

                            // Detailed Rationale Message (Message from risk level)

                            Text(
                                text = modelDetailedSummary ?: "**No detailed summary provided.**",
                                style = MaterialTheme.typography.bodyMedium
                            )

                        }
                    }
                    Spacer(Modifier.height(15.dp))
                }


                Text(
                    "Patient's Self-Assessment (Questionnaire)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(8.dp))


                when {
                    loading -> {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading…", color = Color.Gray)
                    }

                    error != null -> {
                        Text(error ?: "Error", color = Color(0xFF8A1C1C))
                    }

                    else -> {
                        if (answers.isEmpty()) {
                            Text("No questionnaire answers found.", color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (i in 1..10) {
                                    val key = "question_$i"
                                    val label = questionLabels.getOrNull(i - 1) ?: key
                                    val value = answers[key] == true

                                    // 🔑 FIX: Correctly map Boolean 'value' to "Yes" or "No"
                                    val displayValue = if (value) "Yes" else "No"

                                    Column {
                                        Text(
                                            text = "Q$i. $label\n",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold // Added bolding for question
                                            )
                                        )
                                        Surface(
                                            color = Color(0xFF0E4C4C).copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = "Answer: $displayValue", // <-- Use displayValue
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 6.dp
                                                )
                                            )
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 5.dp))

                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        PrimaryButton(
                            text = "Close",
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(46.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 🔑 FIX: Scroll Down Hint is correctly positioned and visible logic is applied
            ScrollDownHint(
                visible = showScrollHint,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp) // Adjusted padding to float above the close button area
            )
        }
    }
}

private fun buildLabelMap(questions: List<String>): Map<String, String> =
    questions.mapIndexed { idx, q -> "question_${idx + 1}" to q }.toMap()

@Composable
private fun AnswersListPretty(
    answers: Map<String, Any>,
    labelMap: Map<String, String>
) {
    // stable sort by numeric suffix if present (question_1..question_10)
    val orderedKeys = answers.keys.sortedWith(compareBy(
        { key -> key.substringAfter("question_", "").toIntOrNull() ?: Int.MAX_VALUE },
        { it }
    ))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        orderedKeys.forEach { key ->
            val label = labelMap[key]
                ?: key.replace("_", " ").replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF0E4C4C)
                )
                Text(
                    text = prettyValue(answers[key]),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun prettyValue(v: Any?): String = when (v) {
    null -> "—"
    is Boolean -> if (v) "Yes" else "No"
    is Number -> v.toString()
    is List<*> -> v.joinToString("\n") { "• ${it ?: "—"}" }
    is Map<*, *> -> v.entries.joinToString("\n") { "• ${it.key}: ${prettyValue(it.value)}" }
    else -> v.toString()
}


@Composable
fun ScrollDownHint(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFBFFDFD),
                            Color(0xFF88E7E7),
                            Color(0xFF55BFBF)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Scroll down",
                tint = Color(0xFF0FB2B2),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun riskLevelLabel(probability: Float): String {
    val p = probability * 100f
    return when {
        // NOTE: 0.0112f is 1.12%
        probability < 0.0112f -> "Very Low"
        p < 10f  -> "Very Low"
        p < 30f  -> "Low"
        p < 60f  -> "Medium" // CHANGED: Was "Moderate"
        p < 80f  -> "High"   // CHANGED: Was "Elevated"
        else     -> "Very High" // CHANGED: Was "High"
    }
}

fun calculateModelSummary(prediction: String?, probability: Double?): String? {
    if (prediction.isNullOrBlank() || probability == null) return null

    // Convert Double from Firestore to Float for user's logic
    val probFloat = probability.toFloat()

    val riskLabel = riskLevelLabel(probFloat)

    // Format probability to percentage for display
    val percent = probFloat * 100f
    val formattedProb = "%.1f".format(Locale.getDefault(), percent)

    // Return the combined summary for the button
    // Example: "BENIGN (Very Low Risk - Prob: 0.4%)"
    return "${prediction.uppercase(Locale.getDefault())} (${riskLabel} Risk - Probability: ${formattedProb}%)"
}
// --- Lesion ID Groups and List Generator ---

object LesionIds {
    val benignIds = listOf(
        "Common benign nevus",
        "Atypical/Dysplastic nevus",
        "Seborrheic keratosis",
        "Solar lentigo",
        "Lichen planus–like keratosis",
        "Dermatofibroma"
    )

    val lt30Ids = listOf(
        "Solar/actinic keratosis",
        "Squamous cell carcinoma in situ",
        "Melanoma in situ (general)",
        "Superficial basal cell carcinoma",
        "Nodular Basal Cell Carcinoma",
        "Indeterminate melanocytic neoplasm"
    )

    val lt60Ids = listOf(
        "Melanoma in situ, lentigo maligna",
        "Melanoma in situ, with nevus",
        "Melanoma invasive, superficial spreading",
        "Basal cell carcinoma (general malignant group)",
        "Melanoma invasive (general)",
        "Atypical intraepithelial melanocytic proliferation"
    )

    val lt80Ids = listOf(
        "Squamous cell carcinoma, invasive",
        "Melanoma, NOS (not otherwise specified)",
        "Basal cell carcinoma (general malignant group)"
    )

    val gte80Ids = listOf(
        "Nodular basal cell carcinoma",
        "Superficial basal cell carcinoma",
        "Melanoma in situ (general)"
    )
}
// In DermaAssessmentReport.kt, near your other helper functions:

private fun getModelRationaleMessage(probability: Double?): String? {
    // Returns null immediately if probability is null.
    if (probability == null) return null

    val probFloat = probability.toFloat()
    val pPct = probFloat * 100f // Probability as Percentage

    // This is the base rationale text, which is the only thing we now return.
    val base = when {
        pPct < 10f -> "Your result shows a very low chance of concern. This is reassuring, and there’s no need to worry. It may help to simply check your skin from time to time, just to stay aware of any changes."
        pPct < 30f -> "Your result suggests only a low chance of concern. Everything appears fine. We encourage you to casually observe your skin every now and then, and let a doctor know if you notice something different."
        pPct < 60f -> "We noticed some minor concern in your skin. This does not mean there is a serious issue, but talking with a doctor could provide peace of mind and helpful guidance."
        pPct < 80f -> "Your result shows some concern. To better understand this, we recommend scheduling a skin check with a dermatologist. They can give you clearer answers and reassurance."
        else       -> "Your result shows a higher level of concern. For your safety and peace of mind, we encourage you to visit a dermatologist soon so you can receive proper care and support."
    }

    return base
}
// --- Helper functions for dynamic styling ---
private fun getRiskColor(riskLabel: String): Color {
    return when (riskLabel.uppercase(Locale.getDefault())) {
        "VERY LOW" -> Color(0xFFE0FFF0)  // Light Green/Teal (Reassuring)
        "LOW" -> Color(0xFFF0FFF0)       // Lighter Green
        "MEDIUM" -> Color(0xFFFFE0A0)  // CHANGED: Was "MODERATE"
        "HIGH" -> Color(0xFFFFB0B0)  // CHANGED: Was "ELEVATED"
        "VERY HIGH" -> Color(0xFFFF8080) // CHANGED: Was "HIGH"
        else -> Color(0xFFE0E0E0)        // Gray (Default)
    }
}

private fun getBorderColor(riskLabel: String): Color {
    return when (riskLabel.uppercase(Locale.getDefault())) {
        "VERY LOW", "LOW" -> Color(0xFF00ADB5) // Teal
        "MEDIUM" -> Color(0xFFFFA500) // CHANGED: Was "MODERATE"
        "HIGH", "VERY HIGH" -> Color(0xFFFF0000) // CHANGED: Was "ELEVATED", "HIGH"
        else -> Color(0xFF888888) // Gray
    }
}