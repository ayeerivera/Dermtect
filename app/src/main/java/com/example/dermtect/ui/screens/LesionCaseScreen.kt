package com.example.dermtect.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun LesionCaseScreen(
    caseId: String,
    onBackClick: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val ctx = LocalContext.current
    val imageLoader = remember { ImageLoader(ctx) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var heatmapBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var title by remember { mutableStateOf("Scan") }
    var probability by remember { mutableStateOf(0f) }
    var timestampText by remember { mutableStateOf("—") }

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

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading case...", color = Color.Gray)
                }
            }
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = Color.Red)
            }
        }
        else -> {
            val tau = 0.0112f
            val alert = probability >= tau
            val riskCopy = generateTherapeuticMessage(probability) // uses only probability

            // If your template still requires a "prediction" string, pass Malignant/Benign
            val uiPrediction = if (alert) "Malignant" else "Benign"

            LesionCaseTemplate(
                imageResId = null,
                imageBitmap = imageBitmap,
                camBitmap = heatmapBitmap,
                title = title,
                timestamp = timestampText,
                riskTitle = "Risk Assessment:",
                riskDescription = riskCopy,
                prediction = if (probability >= 0.0112f) "Malignant" else "Benign",
                probability = probability,
                onBackClick = onBackClick,

                // New:
                showPrimaryButtons = false,
                showSecondaryActions = true,

                onDownloadClick = { /* open/generate PDF */ },
                onFindClinicClick = { /* nearby clinics */ }
            )

        }

    }
}