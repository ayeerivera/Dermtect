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
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
 import androidx.compose.runtime.Composable
 import androidx.compose.material3.Card


@Composable
fun DermaAssessmentScreenReport(
    caseId: String,
    onBackClick: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var scanTitle by remember { mutableStateOf("Scan") }
    var lesionImageUrl by remember { mutableStateOf<String?>(null) }
    var diagnosis by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var realCaseId by remember { mutableStateOf<String?>(null) }
    val canSave by remember { derivedStateOf { !diagnosis.isNullOrBlank() } }

    var assessedScanNo by remember { mutableStateOf<Long?>(null) }
    var assessedAt by remember { mutableStateOf<com.google.firebase.Timestamp?>(null) }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showRiskSheet by remember { mutableStateOf(false) }

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

            // parent fields
            scanTitle      = finalDoc.getString("label") ?: "Scan"
            lesionImageUrl = finalDoc.getString("scan_url")
            assessedScanNo = finalDoc.getLong("assessed_scan_no")
            assessedAt     = finalDoc.getTimestamp("assessed_at")

            // subdoc (existing assessment)
            val assess = cases.document(realCaseId!!)
                .collection("derma_assessment")
                .document("latest")
                .get(Source.SERVER)
                .await()

            if (assess.exists()) {
                diagnosis = assess.getString("diagnosis")
                notes = assess.getString("notes") ?: ""
                // fallback date if parent missing
                if (assessedAt == null) {
                    assessedAt = assess.getTimestamp("assessed_at")
                }
            } else {
                diagnosis = null
                notes = ""
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
        Box(Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 10.dp)
            ) {
                BackButton(
                    onClick = { showDiscardDialog = true },
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
                        text = dateText,   // ← shows formatted “MMM dd, yyyy • HH:mm” or "—"
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                    )
                    Spacer(Modifier.height(15.dp))

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
                            .padding(top = 110.dp, bottom = 50.dp, start = 20.dp, end = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Image
                        val imageSide = 280.dp // match user screen feel
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(imageSide)        // square, not just height
                                .fillMaxWidth(),        // keeps centering; size wins for height
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
                        AdditionalContextCard(
                            onClick = { showRiskSheet = true }
                        )

                        Spacer(Modifier.height(20.dp))

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
                                    enabled = true
                                )
                            } else {
                                SecondaryButton(
                                    text = "Benign",
                                    onClick = { diagnosis = "Benign" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true
                                )
                            }

                            if (malignantSelected) {
                                PrimaryButton(
                                    text = "Malignant",
                                    onClick = { diagnosis = "Malignant" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true
                                )
                            } else {
                                SecondaryButton(
                                    text = "Malignant",
                                    onClick = { diagnosis = "Malignant" },
                                    modifier = Modifier.weight(1f).height(btnHeight),
                                    enabled = true
                                )
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
                                if (new.length <= maxChars) {
                                    notes = new
                                    notesError = null
                                } else {
                                    notesError = "Max $maxChars characters"
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "Write your observations…",
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),   // comfy multi-line
                            minLines = 4,
                            maxLines = 8,
                            isError = notesError != null,
                            supportingText = {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (notesError != null) Text(notesError!!, color = MaterialTheme.colorScheme.error)
                                    Text("${notes.length}/$maxChars")
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrect = true
                            )
                        )


                        // Action buttons (vertically stacked)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 12.dp), // was 24
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp) // was 12
                        ) {
                            // Save (Primary when enabled)
                            PrimaryButton(
                                text = if (saving) "Saving…" else "Submit Assessment",
                                onClick = {
                                    if (!canSave || saving) return@PrimaryButton
                                    val id = realCaseId ?: run { error = "Missing case id."; return@PrimaryButton }
                                    val dermaId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                                        error = "Not signed in."; return@PrimaryButton
                                    }
                                    saving = true
                                    scope.launch {
                                        try {
                                            db.collection("lesion_case").document(id)
                                                .collection("derma_assessment").document("latest")
                                                .set(
                                                    mapOf(
                                                        "diagnosis" to diagnosis,                 // "Benign" | "Malignant"
                                                        "notes" to notes,
                                                        "assessedBy" to dermaId,                  // derma uid
                                                        "assessed_at" to FieldValue.serverTimestamp(),
                                                        "status" to "completed"                   // ✅ subdoc status only
                                                    ),
                                                    SetOptions.merge()
                                                ).await()
                                            onBackClick()
                                        } catch (t: Throwable) {
                                            saving = false
                                            error = "Save failed: ${t.message}"
                                        }
                                    }
                                },
                                enabled = canSave && !saving,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(0.9f).padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // …PrimaryButton (Submit) above…

                                SecondaryButton(
                                    text = "Cancel",
                                    onClick = {
                                        showDiscardDialog = true
                                        val id = realCaseId ?: run { onBackClick(); return@SecondaryButton }
                                        val dermaId = FirebaseAuth.getInstance().currentUser?.uid
                                        scope.launch {
                                            try {
                                                db.collection("lesion_case").document(id)
                                                    .collection("derma_assessment").document("latest")
                                                    .set(
                                                        mapOf(
                                                            // keep any drafts if you want, or omit them:
                                                            "diagnosis" to (diagnosis ?: FieldValue.delete()),
                                                            "notes" to (if (notes.isBlank()) FieldValue.delete() else notes),
                                                            "assessedBy" to (dermaId ?: FieldValue.delete()),
                                                            "assessed_at" to FieldValue.serverTimestamp(),
                                                            "status" to "pending"              // ✅ subdoc status only
                                                        ),
                                                        SetOptions.merge()
                                                    ).await()
                                            } catch (_: Throwable) {}
                                            onBackClick()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )
                            }
                    }

                    }
                }
            }
            if (showRiskSheet) {
                RiskAssessmentSheet(
                    caseId = realCaseId,                 // can be null until loaded
                    onDismiss = { showRiskSheet = false }
                )
            }
            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            // 🔹 Reset local states
                            diagnosis = null
                            notes = ""

                            // 🔹 Mark the case as pending again in Firestore
                            val caseId = realCaseId ?: return@TextButton
                            val db = FirebaseFirestore.getInstance()
                            val caseRef = db.collection("lesion_case").document(caseId)

                            caseRef.update("status", "pending")
                                .addOnSuccessListener {
                                    Log.d("DermaAssessment", "Case marked as pending again")
                                }

                            // 🔹 Close dialog and navigate back
                            showDiscardDialog = false
                            onBackClick()
                        }) {
                            Text("Discard", color = Color(0xFF8A1C1C))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Discard changes?") },
                    text = { Text("Your changes will not be saved. Do you want to discard them and mark this case as pending?") }
                )

        }}
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

suspend fun saveDermaAssessment(
    caseId: String,
    diagnosis: String,
    notes: String,
    db: com.google.firebase.firestore.FirebaseFirestore
) {
    val data = mapOf(
        "diagnosis" to diagnosis,
        "notes" to notes,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
    // nested under the case:
    db.collection("lesion_case")
        .document(caseId)
        .collection("derma_assessment")
        .document("latest")
        .set(data, com.google.firebase.firestore.SetOptions.merge())
        .await()
}

// Firestore fetch helper (collection name: "dermaReports" — change if needed)
suspend fun fetchDermaReportById(
    reportId: String,
    db: com.google.firebase.firestore.FirebaseFirestore
): DermaReport? {
    val snap = db.collection("dermaReports").document(reportId).get().await()
    if (!snap.exists()) return null
    return DermaReport(
        id = snap.id,
        assessedScanNo = snap.getLong("assessed_scan_no"),
        assessedAt = snap.getTimestamp("assessed_at"),
        assessmentResult = snap.getString("assessmentResult") ?: "—",
        notes = snap.getString("notes") ?: "—",
        imageUrl = snap.getString("imageUrl") ?: ""
    )
}


private suspend fun saveFinalAssessmentToCase(
    db: FirebaseFirestore,
    caseId: String,
    diagnosis: String,
    notes: String,
    dermaUid: String
) {
    val caseRef = db.collection("lesion_case").document(caseId)
    val metaRef = db.collection("derma_meta").document(dermaUid) // per-derma counter

    // optional: keep a per-derma running Scan # using a transaction
    db.runTransaction { tx ->
        val metaSnap = tx.get(metaRef)
        val next = (metaSnap.getLong("next_scan_no") ?: 1L)
        tx.set(metaRef, mapOf("next_scan_no" to next + 1), SetOptions.merge())

        tx.update(caseRef, mapOf(
            "assessor_id" to dermaUid,
            "assessed_scan_no" to next,                 // ← “Scan #” for this derma
            "assessed_at" to FieldValue.serverTimestamp(),
            "derma_diagnosis" to diagnosis,
            "derma_notes" to notes,
            "assessment_status" to "completed"          // your status field on the case doc
        ))

        // (optional) also write/append to subcollection for history
        tx.set(
            caseRef.collection("derma_assessment").document("latest"),
            mapOf(
                "diagnosis" to diagnosis,
                "notes" to notes,
                "assessed_at" to FieldValue.serverTimestamp(),
                "assessor_id" to dermaUid,
                "status" to "completed"
            ),
            SetOptions.merge()
        )
    }.await()
}

suspend fun markPending(
    caseId: String,
    db: FirebaseFirestore
) {
    // Keep “pending” state inside the derma_assessment doc (no need to update parent doc,
    // since rules don’t allow dermas to change lesion_case fields)
    val data = mapOf(
        "status" to "pending",
        "updatedAt" to FieldValue.serverTimestamp()
    )
    db.collection("lesion_case")
        .document(caseId)
        .collection("derma_assessment")
        .document("latest")
        .set(data, SetOptions.merge())
        .await()
}
@Composable
fun AdditionalContextCard(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0FBF7)),
        border = BorderStroke(1.dp, Color(0xFFB7FFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top label: "Additional Context" with info icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF0FB2B2),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Additional Context",
                    color = Color(0xFF0FB2B2),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Main clickable label
            Text(
                text = "View Risk Assessment & Report",
                color = Color(0xFF0E4C4C),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAssessmentSheet(
    caseId: String?,
    onDismiss: () -> Unit
) {

    if (caseId == null) {
        // If we don't have the case yet, just close gracefully
        onDismiss()
        return
    }

    val db = remember { FirebaseFirestore.getInstance() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pdfUrl by remember { mutableStateOf<String?>(null) }

    // Store answers as key → value
    var answers by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    val context = LocalContext.current
    LaunchedEffect(caseId) {
        loading = true
        error = null
        try {
            // 1) Try subdoc with answers
            val ref = db.collection("lesion_case")
                .document(caseId)
                .collection("risk_assessment")
                .document("latest")

            val snap = ref.get(Source.SERVER).await()
            if (snap.exists()) {
                answers = snap.data?.filterKeys { it != "report_pdf_url" } ?: emptyMap()
                pdfUrl = snap.getString("report_pdf_url")
            } else {
                // 2) Fallback: maybe PDF URL is on parent doc
                val parent = db.collection("lesion_case").document(caseId).get(Source.SERVER).await()
                pdfUrl = parent.getString("report_pdf_url")
                answers = emptyMap()
            }
        } catch (t: Throwable) {
            error = "Failed to load assessment: ${t.message}"
        } finally {
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Risk Assessment",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))

            when {
                loading -> {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading…", color = Color.Gray)
                }
                error != null -> {
                    Text(error ?: "Error", color = Color(0xFF8A1C1C))
                }
                else -> {
                    // Answers section (if any)
                    if (answers.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        AnswersList(answers)
                        Spacer(Modifier.height(14.dp))
                    } else {
                        Text(
                            "No questionnaire answers found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // PDF actions
                    PrimaryButton(
                        text = if (pdfUrl != null) "Open PDF Report" else "Generate PDF",
                        onClick = {
                            if (pdfUrl != null) {
                                pdfUrl?.let { openPdfUrl(context, it) }   // ✅ use captured context
                            } else {
                                // 🔧 If you already have a generator, call it here.
                                // e.g., trigger cloud function or your PdfExporter and then refresh.
                                // For now, just show a toast or set error.
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp),
                        enabled = true
                    )

                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        text = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(0.9f).height(46.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
@Composable
private fun AnswersList(answers: Map<String, Any>) {
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
        answers.forEach { (key, value) ->
            Column {
                Text(
                    text = key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF0E4C4C)
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun openPdfUrl(context: android.content.Context, url: String) {
    // If you want to render in-app: consider WebView + Google viewer.
    // For now, use ACTION_VIEW so user’s PDF viewer or browser handles it.
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // fallback: open via browser
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browser)
    }
}
