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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties


@Composable
fun LesionCaseScreen(
    caseId: String,
    onBackClick: () -> Unit,
    onFindClinicClick: () -> Unit
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

    val questions = remember {
        listOf(
            "Have you noticed this skin spot recently appearing or changing in size?",
            "Does the lesion have uneven or irregular borders?",
            "Is the color of the spot unusual (black, blue, red, or a mix of colors)?",
            "Has the lesion been bleeding, itching, or scabbing recently?",
            "Is there a family history of skin cancer or melanoma?",
            "Has the lesion changed in color or texture over the last 3 months?",
            "Is the lesion asymmetrical (one half unlike the other)?",
            "Is the diameter larger than 6mm (about the size of a pencil eraser)?"
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
                val riskCopy = generateTherapeuticMessage(probability)
                val uiPrediction = if (alert) "Malignant" else "Benign"

                LesionCaseTemplate(
                    imageResId = null,
                    imageBitmap = imageBitmap,
                    camBitmap = heatmapBitmap,
                    title = title,
                    timestamp = timestampText,
                    riskTitle = "Risk Assessment:",
                    riskDescription = riskCopy,
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
                                snackbarHostState.showSnackbar(
                                    "Please complete the questionnaire before downloading the PDF."
                                )
                                return@launch
                            }

                            // Build (Q → Yes/No) pairs for the PDF, using the same order
                            val answerPairs: List<Pair<String, String>> = questions.indices.map { i ->
                                val a = qa!![i] ?: false
                                questions[i] to if (a) "Yes" else "No"
                            }

                            try {
                                val photo = imageBitmap
                                if (photo == null) {
                                    snackbarHostState.showSnackbar("No photo available for PDF.")
                                    return@launch
                                }

                                val data = PdfExporter.CasePdfData(
                                    title = title,
                                    timestamp = timestampText,
                                    photo = photo,
                                    heatmap = heatmapBitmap,
                                    shortMessage = generateTherapeuticMessage(probability),
                                    answers = answerPairs       // 👈 add the formatted answers here
                                )

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

            }

        }
    }
}

@Composable
private fun ZoomableImage(
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
        // ✕ Close ABOVE the photo
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
                        color = Color.White, // keep white like BackButton
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


            // Zoomable square image
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
}
