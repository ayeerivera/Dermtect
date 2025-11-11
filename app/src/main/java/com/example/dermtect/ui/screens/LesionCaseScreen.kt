package com.example.dermtect.ui.screens

import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.util.Log
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

    // --- UI state
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var fullImagePage by remember { mutableStateOf<Int?>(null) } // 0 = photo, 1 = heatmap
    var showQaRequiredDialog by remember { mutableStateOf(false) }  // ✅ ADD
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }

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
                val tau = 0.0112f
                val alert = probability >= tau
                val uiPrediction = if (alert) "Malignant" else "Benign"

                LesionCaseTemplate(
                    imageResId = null,
                    imageBitmap = imageBitmap,
                    camBitmap = heatmapBitmap,
                    title = title,
                    timestamp = timestampText,
                    riskTitle = "Risk Assessment",
                    riskDescription = "", // not used anymore
                    prediction = uiPrediction,
                    probability = probability,
                    onBackClick = onBackClick,
                    showPrimaryButtons = false,
                    showSecondaryActions = true,


                    onDownloadClick = {
                        scope.launch {
                            // ✅ Gate on questionnaire completion (user-level doc)
                            val qa = existingAnswers
                            val notCompleted = (qa == null) || qa.any { it == null }
                            if (notCompleted) {
                                showQaRequiredDialog =
                                    true    // ✅ show the clickable dialog instead of snackbar
                                return@launch
                            }


                            // Build (Q → Yes/No) pairs for the PDF, using the same order
                            val answerPairs: List<Pair<String, String>> =
                                questions.indices.map { i ->
                                    val a = qa!![i] ?: false
                                    questions[i] to if (a) "Yes" else "No"
                                }

                            try {
                                val photo = imageBitmap
                                if (photo == null) {
                                    snackbarHostState.showSnackbar("No photo available for PDF.")
                                    return@launch
                                }
                                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (userId == null) {
                                    snackbarHostState.showSnackbar("You must be signed in to export.")
                                    return@launch
                                }

// sequential per-user ID (suspend)
                                // sequential per-user ID (suspend)
                                val reportId = PdfExporter.nextUserReportSeq(userId)

// ❗ Use the Firestore-loaded probability for this screen
                                val prob = probability
                                val pPct = prob * 100f
                                val alerted = prob >= 0.0112f

// ✅ Build the exact same 6-item (or bucketed) list you show in UI
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

// ✅ Compact summary for the PDF (no bullets here)
                                val pdfSummary = if (!alerted) {
                                    "This scan looks reassuring, with a very low likelihood of a serious issue."
                                } else {
                                    when {
                                        pPct < 10f -> "Very low chance of concern. Casual self-checks are enough."
                                        pPct < 30f -> "Low chance of concern. Keep an eye on changes."
                                        pPct < 60f -> "Minor concern. Consider discussing with a doctor."
                                        pPct < 80f -> "Moderate concern. We recommend a dermatologist visit."
                                        else        -> "Higher concern. Please visit a dermatologist soon."
                                    }
                                }

                                val data = PdfExporter.CasePdfData(
                                    reportId = reportId,
                                    title = title,
                                    userFullName = "John Dela Cruz",
                                    timestamp = timestampText,
                                    photo = photo,
                                    heatmap = heatmapBitmap,
                                    shortMessage = pdfSummary,          // 👈 concise text (no bullets)
                                    possibleConditions = idsToShow,     // 👈 THIS passes the 6 items to the PDF
                                    answers = answerPairs
                                )


                                val uri = withContext(Dispatchers.IO) {
                                    PdfExporter.createCasePdf(ctx, data)
                                }
                                PdfExporter.openPdf(ctx, uri)


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
