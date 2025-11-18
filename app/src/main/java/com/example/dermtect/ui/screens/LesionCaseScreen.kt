package com.example.dermtect.ui.screens

import android.content.Intent
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.example.dermtect.pdf.PdfExporter
import com.example.dermtect.ui.viewmodel.QuestionnaireViewModel
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import com.example.dermtect.tflt.DermtectResult


@Composable
fun LesionCaseScreen(
    navController: NavController,
    caseId: String,
    onBackClick: () -> Unit,
    onFindClinicClick: () -> Unit,
    onNavigateToAssessment: () -> Unit
) {
    // --- Essentials
    val db = remember { FirebaseFirestore.getInstance() }
    val ctx = LocalContext.current
    val imageLoader = remember { ImageLoader(ctx) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var fullImagePage by remember { mutableStateOf<Int?>(null) } // 0 = photo, 1 = heatmap
    var showQaRequiredDialog by remember { mutableStateOf(false) }  // ✅ ADD
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }
    var scanUrl by remember { mutableStateOf<String?>(null) }
    var heatmapUrl by remember { mutableStateOf<String?>(null) }
    var timestampMs by remember { mutableStateOf<Long?>(null) }
    var dermaStatus by remember { mutableStateOf<String?>(null) }
    var dermaNotes by remember { mutableStateOf<String?>(null) }
    var dermaName by remember { mutableStateOf<String?>(null) } // 👈 add this
    var dermaDisplayName by remember { mutableStateOf<String?>(null) }

    val questions = remember {
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
    val qvm = remember { QuestionnaireViewModel() }
    val existingAnswers by qvm.existingAnswers.collectAsState() // List<Boolean?>? or null

    // --- Case data state
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var heatmapBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("Scan") }
    var probability by remember { mutableStateOf(0f) }
    var timestampText by remember { mutableStateOf("—") }

    // --- Questionnaire answers state
    var answers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        qvm.loadQuestionnaireAnswers()  // uses current user UID per your VM
    }


    LaunchedEffect(caseId) {
        loading = true
        error = null
        try {
            // 1) Fetch Firestore doc
            val doc = db.collection("lesion_case").document(caseId).get().await()
            if (!doc.exists()) throw IllegalStateException("Case not found")

            title = doc.getString("label") ?: "Scan"
            probability = (doc.getDouble("probability") ?: 0.0).toFloat()

            val ts = doc.getTimestamp("timestamp")?.toDate()
            timestampText = ts?.let {
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
            } ?: "—"

            val photoUrl = doc.getString("scan_url")
            val heatUrl = doc.getString("heatmap_url")

            dermaStatus = doc.getString("derma_status") ?: doc.getString("status")
            dermaNotes = doc.getString("notes")
            // ✅ Prefer cached name on the case doc
            dermaName = doc.getString("assessor_display_name")
// ✅ Final: resolve derma name only if missing
            if (dermaName.isNullOrBlank()) {
                val assessorId = doc.getString("assessor_id") ?: doc.getString("assessorId")
                if (!assessorId.isNullOrBlank()) {
                    dermaName = try {
                        val pub = db.collection("profiles_public").document(assessorId).get().await()
                        pub.getString("displayName") ?: pub.getString("fullName")
                    } catch (_: Throwable) { null }
                        ?: run {
                            try {
                                val u = db.collection("users").document(assessorId).get().await()
                                u.getString("full_name")?.trim().takeUnless { it.isNullOrBlank() }
                                    ?: listOfNotNull(
                                        u.getString("firstName")?.trim(),
                                        u.getString("lastName")?.trim()
                                    ).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
                            } catch (_: Throwable) { null }
                        }
                }
            }

            // 2) Helper for Coil bitmap load
            suspend fun loadBitmap(url: String?): Bitmap? {
                if (url.isNullOrBlank()) return null
                val req = ImageRequest.Builder(ctx)
                    .data(url)
                    .allowHardware(false)   // we nee d a software bitmap
                    .size(Size.ORIGINAL)
                    .build()
                val result = withContext(Dispatchers.IO) { imageLoader.execute(req) }
                val drawable = result.drawable ?: return null
                return drawable.toBitmap()
            }

            // 3) Download both images
            imageBitmap = loadBitmap(photoUrl)
            heatmapBitmap = loadBitmap(heatUrl)

        } catch (e: Exception) {
            Log.e("LesionCaseScreen", "Load failed", e)
            error = e.message ?: "Failed to load case"
        } finally {
            loading = false
        }
    }

    Scaffold( // ADD: gives us a Snackbar host for user feedback
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading case...", color = Color.Gray)
                    }
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color.Red)
                }
            }

            else -> {
                val hasDerma = dermaStatus == "completed" && !dermaNotes.isNullOrBlank()

                val tau = 0.0112f
                val alert = probability >= tau

                LesionCaseTemplate(
                    imageResId = null,
                    imageBitmap = imageBitmap,
                    camBitmap = heatmapBitmap,
                    title = title,
                    timestamp = timestampText,
                    riskTitle = "",
                    riskDescription = "", // not used anymore
                    prediction = "",
                    probability = probability,
                    showRiskSection = !hasDerma,
                    dermaNotes = dermaNotes,
                    dermaName = dermaName,
                    onBackClick = onBackClick,
                    showPrimaryButtons = false,
                    showSecondaryActions = true,


                    onDownloadClick = {
                        scope.launch {
                            // 1) Gate on questionnaire completion
                            val qa = existingAnswers
                            val notCompleted = (qa == null) || qa.any { it == null }
                            if (notCompleted) {
                                showQaRequiredDialog = true
                                return@launch
                            }

                            // 2) Ensure we have the photo
                            val photo = imageBitmap
                            if (photo == null) {
                                snackbarHostState.showSnackbar("No photo available for PDF.")
                                return@launch
                            }

                            // 3) Ensure signed in
                            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (userId == null) {
                                snackbarHostState.showSnackbar("You must be signed in to export.")
                                return@launch
                            }

                            try {
                                // 4) Build (Q → Yes/No) pairs in the same order
                                val answerPairs: List<Pair<String, String>> =
                                    questions.indices.map { i ->
                                        val a = qa!![i] ?: false
                                        questions[i] to if (a) "Yes" else "No"
                                    }

                                // 5) Pull user profile (optional but nice to have on PDF)
                                val userSnap = FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .get()
                                    .await()

                                val first = userSnap.getString("firstName").orEmpty()
                                val last  = userSnap.getString("lastName").orEmpty()
                                val birthdayStr = userSnap.getString("birthday") // may be null
                                val fullName = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "—" }

                                // 6) Compute the short code ONLY for the PDF (no UI badge)
                                val shortCode = PdfExporter.shortReportFrom(caseId = caseId, fallback = "TEMP0")

                                // 7) Recreate the same “possible conditions” list used in UI
                                val prob = probability
                                val pPct = prob * 100f
                                val alerted = prob >= 0.0112f
                                val idsToShow: List<String> = if (!alerted) {
                                    LesionIds.benignIds
                                } else {
                                    when {
                                        pPct < 10f -> LesionIds.benignIds
                                        pPct < 30f -> LesionIds.lt30Ids
                                        pPct < 60f -> LesionIds.lt60Ids
                                        pPct < 80f -> LesionIds.lt80Ids
                                        else       -> LesionIds.gte80Ids
                                    }
                                }

                                // 8) Compact summary text (no bullets; bullets come from possibleConditions)
                                val pdfSummary = if (!alerted) {
                                    "This scan looks reassuring, with a very low likelihood of a serious issue. " +
                                            "You can continue your normal skincare routine. However, it's still a good idea to consult a dermatologist for proper evaluation. " +
                                            "If you notice any changes in the spot within 2 to 4 weeks, such as growth, darkening, itching, or bleeding, please schedule a check-up."
                                } else {
                                    when {
                                        pPct < 10f -> "Your result shows a very low chance of concern. This is reassuring, and there’s no need to worry. " +
                                                "Still, we recommend consulting a dermatologist for proper assessment. " +
                                                "If the mole or spot changes in the next 2 to 4 weeks, it's best to get it checked."

                                        pPct < 30f -> "Your result suggests a low chance of concern. Everything appears fine. " +
                                                "For safety, we still encourage visiting a dermatologist for confirmation. " +
                                                "Monitor the area and consult a doctor if you notice changes within 1 to 3 weeks."
                                        pPct < 60f -> "We noticed a minor concern in your scan. This does not mean there is a serious issue, but consulting a dermatologist can provide clarity and peace of mind. " +
                                                "Please also monitor the spot for any changes within 1 to 3 weeks."
                                        pPct < 80f -> "Your result shows some concern. To better understand this finding, we recommend scheduling a skin check with a dermatologist. " +
                                                "If the area changes in appearance over the next 1 to 2 weeks, please seek medical care sooner."

                                        else       -> "Your result shows a higher level of concern. For your safety and peace of mind, we strongly encourage you to visit a dermatologist soon for proper evaluation. " +
                                                "If you notice any changes in the spot within 1 to 2 weeks, seek medical attention immediately."
                                    }
                                }


                                // 9) Build PDF payload (short code goes into reportId)
                                val data = PdfExporter.CasePdfData(
                                    reportId = shortCode,            // ✅ short 4-char ID, PDF-only
                                    title = title,
                                    userFullName = fullName,
                                    birthday = birthdayStr,
                                    timestamp = timestampText,
                                    photo = photo,
                                    heatmap = heatmapBitmap,         // can be null
                                    shortMessage = pdfSummary,
                                    possibleConditions = idsToShow,
                                    answers = answerPairs
                                )

                                // 10) Create & open the PDF locally (no cloud upload here)
                                val uri = withContext(Dispatchers.IO) {
                                    PdfExporter.createCasePdf(ctx, data)
                                }
                                PdfExporter.openPdf(ctx, uri)
                                snackbarHostState.showSnackbar("PDF saved successfully")
                            } catch (t: Throwable) {
                                Log.e("LesionCaseScreen", "PDF export failed", t)
                                snackbarHostState.showSnackbar("Failed to create PDF: ${t.message ?: "Unknown error"}")
                            }
                        }
                    },
                    onFindClinicClick = onFindClinicClick,
                    onImageClick = { page -> fullImagePage = page }
                )


                if (fullImagePage != null) {
                    Dialog(
                        onDismissRequest = { fullImagePage = null },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,   // ⬅️ no width cap; let us fill the screen
                            decorFitsSystemWindows = false     // ⬅️ avoid system-bar insets shrinking content
                        )
                    ) {
                        // Fullscreen, zero padding, very light black overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.10f)) // 10% opacity
                                .clickable { fullImagePage = null },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = when (fullImagePage) {
                                0 -> imageBitmap
                                1 -> heatmapBitmap ?: imageBitmap
                                else -> null
                            }

                            bmp?.let {
                                ZoomableImage(
                                    bitmap = it,
                                    onClose = { fullImagePage = null }
                                )
                            } ?: Text("No image", color = Color.White)

                        }
                    }
                }
                if (showQaRequiredDialog) {
                    com.example.dermtect.ui.components.DialogTemplate(
                        show = showQaRequiredDialog,
                        title = "Complete Assessment Required",
                        description = "You can tap the button above, or go manually to:\n Settings → Profile → My Assessment Report",
                        primaryText = "Go to Assessment Report",
                        onPrimary = {
                            showQaRequiredDialog = false
                            onNavigateToAssessment()
                        },
                        secondaryText = "Cancel",
                        onSecondary = { showQaRequiredDialog = false },
                        onDismiss = { showQaRequiredDialog = false },
                        extraContent = {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(
                                        8.dp
                                    )
                                )

                                // 👇 "Why this matters" link that navigates to Terms & Privacy screen
                                Text(
                                    text = "🔗 Why this matters",
                                    color = Color(0xFF0FB2B2),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            showQaRequiredDialog = false
                                            navController.navigate("terms_privacy")
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    )
                }
                LaunchedEffect(Unit) {
                    // When leaving the screen, clear the gate so the user can reopen it later
                    navController.currentBackStackEntry?.savedStateHandle?.set("__last_case_nav__", null)
                }

            }

        }
    }
}

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✕ Close ABOVE the photo (right aligned)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFBFFDFD),
                                Color(0xFF88E7E7),
                                Color(0xFF55BFBF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // ✅ Image is now OUTSIDE the Row, so it won't push the button left
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // keep square
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                        val newOffset = if (newScale > 1f) offset + pan else Offset.Zero
                        scale = newScale
                        offset = newOffset
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}