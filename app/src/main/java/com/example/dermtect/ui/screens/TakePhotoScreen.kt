package com.example.cameradermtect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.dermtect.R
import com.example.dermtect.tflt.DermtectResult
import com.example.dermtect.tflt.TfLiteService
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.screens.LesionCaseTemplate
import com.example.dermtect.ui.screens.generateTherapeuticMessage
import com.example.dermtect.data.repository.ScanRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.geometry.Size as GeometrySize
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.example.dermtect.pdf.PdfExporter
import com.example.dermtect.ui.viewmodel.QuestionnaireViewModel
import kotlinx.coroutines.launch
import kotlin.collections.any
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.ui.zIndex
import com.example.dermtect.util.SkinGateResult
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.dermtect.util.SkinGateConfig
import com.example.dermtect.util.analyzeImageForSkin
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// ---------- Small utilities ----------
fun nowTimestamp(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

// Focus box constants
val FOCUS_BOX = 320.dp
val FOCUS_BOX_WIDTH = FOCUS_BOX
val FOCUS_BOX_HEIGHT = FOCUS_BOX
private val BottomBarHeight = 120.dp   // matches your bottom bar height
private val StatusGap = 12.dp
@Composable
fun TakePhotoScreen(
    onBackClick: () -> Unit = {},
    onFindClinicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val tfService = remember { TfLiteService.get(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var modelFlag by remember { mutableStateOf("Benign") }  // "Malignant" | "Benign"

    var lensFacing = CameraSelector.LENS_FACING_BACK
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) } // OFF/ON/AUTO
    var torchEnabled by remember { mutableStateOf(false) }                    // continuous light for preview

    val hasFlashUnit by remember {
        derivedStateOf { cameraInfo?.hasFlashUnit() == true }
    }

    val previewView =
        remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val coroutineScope = rememberCoroutineScope()

    var hasSaved by remember { mutableStateOf(false) }
    var hasUploaded by remember { mutableStateOf(false) }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var squareRect by remember { mutableStateOf<Rect?>(null) }

    var isRunning by remember { mutableStateOf(false) }
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }

    var fullImagePage by remember { mutableStateOf<Int?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var liveGateResult by remember { mutableStateOf<SkinGateResult?>(null) }
    var canCapture by remember { mutableStateOf(false) }

    // === NEW: questionnaire state (gate PDF download) ===
    val qvm = remember { QuestionnaireViewModel() }
    val existingAnswers by qvm.existingAnswers.collectAsState()
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
    LaunchedEffect(Unit) { qvm.loadQuestionnaireAnswers() }

    // ===== Camera bind =====
    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode) // <- use state
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(224, 224)) // small & fast
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { ia ->
                ia.setAnalyzer(cameraExecutor) { proxy ->
                    try {
                        val bmp = proxy.toBitmapFast()
                        val gate = analyzeImageForSkin(bmp, SkinGateConfig())
                        liveGateResult = gate
                        canCapture = gate.accepted
                    } catch (t: Throwable) {
                        Log.e("SkinGate", "Analyzer error", t)
                        canCapture = false
                    } finally {
                        proxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo

            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("CameraX", "Use case binding failed", exc)
        }

        onDispose {
            cameraProvider.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    LaunchedEffect(torchEnabled, cameraControl, hasFlashUnit) {
        if (hasFlashUnit) {
            cameraControl?.enableTorch(torchEnabled)
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                        val newZoom = (currentZoomRatio * zoomChange).coerceIn(
                            cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f,
                            cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5f
                        )
                        cameraControl?.setZoomRatio(newZoom)
                    }
                }
        )

        // Focus overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.7f), size = size)

                val rectWidth = FOCUS_BOX_WIDTH.toPx()
                val rectHeight = FOCUS_BOX_HEIGHT.toPx()

                val left = (size.width - rectWidth) / 2
                val top = (size.height - rectHeight) / 2
                val right = left + rectWidth
                val bottom = top + rectHeight

                squareRect = Rect(left, top, right, bottom)

                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = GeometrySize(rectWidth, rectHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                val lineLength = 40.dp.toPx()
                val strokeWidth = 6f
                // corners
                drawLine(Color.Cyan, Offset(left, top), Offset(left + lineLength, top), strokeWidth)
                drawLine(Color.Cyan, Offset(left, top), Offset(left, top + lineLength), strokeWidth)
                drawLine(
                    Color.Cyan,
                    Offset(right, top),
                    Offset(right - lineLength, top),
                    strokeWidth
                )
                drawLine(
                    Color.Cyan,
                    Offset(right, top),
                    Offset(right, top + lineLength),
                    strokeWidth
                )
                drawLine(
                    Color.Cyan,
                    Offset(left, bottom),
                    Offset(left + lineLength, bottom),
                    strokeWidth
                )
                drawLine(
                    Color.Cyan,
                    Offset(left, bottom),
                    Offset(left, bottom - lineLength),
                    strokeWidth
                )
                drawLine(
                    Color.Cyan,
                    Offset(right, bottom),
                    Offset(right - lineLength, bottom),
                    strokeWidth
                )
                drawLine(
                    Color.Cyan,
                    Offset(right, bottom),
                    Offset(right, bottom - lineLength),
                    strokeWidth
                )
            }
        }

        // Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 23.dp)
        ) {
            BackButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = { onBackClick() }
            )
        }

        // Title + instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Take a photo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "3–6 inches or 8–15 cm away from the lesion",
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }
        liveGateResult?.let { gate ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    // sit a little higher than the bottom bar (120.dp) by ~24dp
                    .padding(bottom = 120.dp + 24.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (gate.accepted) "Ready to capture"
                    else gate.reason,
                    color = if (gate.accepted) Color(0xFF00C853) else Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom controls: center camera + right flash toggle (cyan, clickable, no circle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFCDFFFF)),
            contentAlignment = Alignment.Center
        ) {
            val flashSupported = cameraInfo?.hasFlashUnit() == true


            // 📸 Camera button (center) — gated by SkinGate
            val canShoot = canCapture
            val enabledColor = Color(0xFFCDFFFF)
            val disabledColor = Color(0xFFBDBDBD)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(65.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    // Outer border for a thicker, more visible stroke effect
                    .border(
                        width = 1.dp, // Thicker outer border for more visibility
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFBFFDFD), // Lighter outer shade
                                Color(0xFF88E7E7), // Medium teal
                                Color(0xFF41A6A6)  // Darker teal
                            )
                        ),
                        shape = CircleShape
                    )
                    .background(if (canShoot) enabledColor else disabledColor, shape = CircleShape)
                    .then(
                        if (canShoot) {
                            Modifier.clickable {
                                // extra safety gate
                                if (!canCapture) return@clickable

                                val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                                imageCapture?.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val fullBitmap = rotateBitmapAccordingToExif(context, file)
                                            squareRect?.let { rect ->
                                                val crop = mapViewRectToImageSquare(
                                                    viewLeft = rect.left,
                                                    viewTop = rect.top,
                                                    viewWidth = rect.width,
                                                    viewHeight = rect.height,
                                                    imageWidth = fullBitmap.width,
                                                    imageHeight = fullBitmap.height,
                                                    previewViewWidth = previewView.width,
                                                    previewViewHeight = previewView.height
                                                )
                                                val side = min(crop.width(), crop.height()).coerceAtLeast(1)
                                                capturedImage = Bitmap.createBitmap(
                                                    fullBitmap, crop.left, crop.top, side, side
                                                )
                                            } ?: run {
                                                val side = min(fullBitmap.width, fullBitmap.height)
                                                val left = (fullBitmap.width - side) / 2
                                                val top = (fullBitmap.height - side) / 2
                                                capturedImage = Bitmap.createBitmap(fullBitmap, left, top, side, side)
                                            }
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                            Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                                        }
                                    }
                                )
                            }
                        } else {
                            // When not allowed, still catch taps to explain why
                            Modifier.clickable {
                                val msg = liveGateResult?.reason ?: "Not ready. Adjust framing/lighting and hold steady."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.camera_vector),
                    contentDescription = "Capture",
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .zIndex(5f)
                    .clickable {
                        // Cycle through flash modes: OFF → ON → AUTO
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        Log.d("UI", "Flash toggled to $flashMode")
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                            else -> Icons.Filled.FlashAuto
                        },
                        contentDescription = "Flash toggle",
                        tint = Color(0xFF0FB2B2),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> "OFF"
                            ImageCapture.FLASH_MODE_ON -> "ON"
                            else -> "AUTO"
                        },
                        color = Color(0xFF0FB2B2),
                        fontSize = 14.sp
                    )
                }
            }
        }



            // Show captured result / analysis
            capturedImage?.let { image ->
                // New image → reset state and run inference
                LaunchedEffect(image) {
                    hasSaved = false
                    hasUploaded = false
                    fullImagePage = null

                    if (!isRunning && inferenceResult == null) {
                        isRunning = true
                        try {
                            val r = withTimeout(TimeoutConfig.INFER_MS) {
                                withContext(Dispatchers.Default) { tfService.infer(image) }
                            }
                            modelFlag = if (r.probability >= 0.0112f) "Malignant" else "Benign"
                            val merged = r.heatmap?.let { overlayBitmaps(image, it, 115) }
                            inferenceResult = r.copy(heatmap = merged)
                        } catch (t: TimeoutCancellationException) {
                            inferenceResult = null
                            Toast.makeText(context, "Analysis took too long. Please retake or try again.", Toast.LENGTH_LONG).show()
                            capturedImage = null
                        } finally {
                            isRunning = false
                        }
                    }
                }


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    when {
                        isRunning -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Analyzing...", color = Color.Gray)
                            }
                        }

                        inferenceResult != null -> {
                            val r = inferenceResult!!
                            val riskCopy = generateTherapeuticMessage(r.probability)


                            LesionCaseTemplate(
                                imageBitmap = image,
                                camBitmap = r.heatmap,
                                title = "Result",
                                timestamp = nowTimestamp(),
                                riskTitle = "Risk Assessment:",
                                riskDescription = riskCopy,
                                prediction = modelFlag,
                                probability = r.probability,

                                showPrimaryButtons = !hasSaved,   // big image before saving
                                showSecondaryActions = hasSaved,   // actions after saving

                                // NEW: tap image to open fullscreen
                                onImageClick = { page ->
                                    fullImagePage = page
                                },  // << was: { showFullImage = true }
                                isSaving = isSaving,
                                onSaveClick = {
                                    coroutineScope.launch {
                                        try {
                                            isSaving = true
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            Log.d(
                                                "TakePhotoScreen",
                                                "onSaveClick fired. uid=$uid, hasImage=${capturedImage != null}, pred=$modelFlag"
                                            )

                                            if (uid == null) {
                                                Toast.makeText(
                                                    context,
                                                    "Please sign in first.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }
                                            if (capturedImage == null) {
                                                Toast.makeText(
                                                    context,
                                                    "No image to save.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }

                                            val ok = uploadScanWithLabel(
                                                bitmap = capturedImage!!,
                                                heatmap = inferenceResult?.heatmap,
                                                probability = inferenceResult?.probability ?: 0f,
                                                prediction = modelFlag
                                            )
                                            Log.d("TakePhotoScreen", "uploadScanWithLabel -> $ok")
                                            if (ok) {
                                                hasSaved = true
                                                Toast.makeText(
                                                    context,
                                                    "Scan saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Save failed (uid or rules).",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (t: Throwable) {
                                            Log.e(
                                                "TakePhotoScreen",
                                                "Save failed with exception",
                                                t
                                            )
                                            Toast.makeText(
                                                context,
                                                "Save failed: ${t.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }finally {
                                            isSaving = false
                                            }
                                    }
                                },
                                onRetakeClick = {
                                    inferenceResult = null
                                    capturedImage = null
                                    hasSaved = false
                                    hasUploaded = false
                                    fullImagePage = null
                                },
                                onBackClick = {
                                    inferenceResult = null
                                    capturedImage = null
                                    hasSaved = false
                                    hasUploaded = false
                                    fullImagePage = null
                                    onBackClick()
                                },
                                onDownloadClick = {
                                    coroutineScope.launch {
                                        // Gate: questionnaire must be complete (8 answers, none null)
                                        val qa = existingAnswers
                                        val notCompleted =
                                            (qa == null) || (qa.size != questions.size) || qa.any { it == null }
                                        if (notCompleted) {
                                            Toast.makeText(
                                                context,
                                                "Please complete the questionnaire before downloading the PDF.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }

                                        try {
                                            val answerPairs: List<Pair<String, String>> =
                                                questions.indices.map { i ->
                                                    val a = qa!![i] ?: false
                                                    questions[i] to if (a) "Yes" else "No"
                                                }

                                            val data = PdfExporter.CasePdfData(
                                                title = "Result",
                                                timestamp = nowTimestamp(),
                                                photo = image,
                                                heatmap = r.heatmap,
                                                shortMessage = generateTherapeuticMessage(r.probability),
                                                answers = answerPairs
                                            )

                                            val uri = withContext(Dispatchers.IO) {
                                                PdfExporter.createCasePdf(context, data)
                                            }
                                            PdfExporter.openPdf(context, uri)
                                            Toast.makeText(
                                                context,
                                                "PDF saved successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } catch (t: Throwable) {
                                            Log.e("TakePhotoScreen", "PDF export failed", t)
                                            Toast.makeText(
                                                context,
                                                "Failed to create PDF: ${t.message ?: "Unknown error"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onFindClinicClick = { onFindClinicClick() }
                            )

                            if (fullImagePage != null) {
                                androidx.compose.ui.window.Dialog(onDismissRequest = {
                                    fullImagePage = null
                                }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val displayBmp = when (fullImagePage) {
                                            0 -> image                       // captured photo
                                            1 -> r.heatmap
                                                ?: image          // heatmap (fallback to photo if null)
                                            else -> null
                                        }
                                        if (displayBmp != null) {
                                            Image(
                                                bitmap = displayBmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { fullImagePage = null },
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text("No image", color = Color.White)
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        }



    // ---------- Helpers (non-Compose) ----------
    fun overlayBitmaps(base: Bitmap, overlay: Bitmap, alpha: Int = 115): Bitmap {
        val config = base.config ?: Bitmap.Config.ARGB_8888
        val result = Bitmap.createBitmap(base.width, base.height, config)
        val canvas = android.graphics.Canvas(result)
        canvas.drawBitmap(base, 0f, 0f, null)

        val paint = android.graphics.Paint().apply { this.alpha = alpha }
        val scaled = if (overlay.width == base.width && overlay.height == base.height)
            overlay
        else
            Bitmap.createScaledBitmap(overlay, base.width, base.height, true)

        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return result
    }

    fun rotateBitmapAccordingToExif(context: Context, file: File): Bitmap {
        val exif = ExifInterface(file.absolutePath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(file))
        val matrix = Matrix().apply { postRotate(rotationAngle) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Uploads the cropped photo to Storage and writes the scan metadata into Firestore:
     * - Storage path: users/{uid}/scans/{caseId}.jpg
     * - Firestore doc: lesion_case/{caseId}
     * Includes a friendly label "Scan N".
     */

    suspend fun uploadScanWithLabel(
        bitmap: Bitmap,            // original cropped photo
        heatmap: Bitmap?,          // heatmap/overlay to save (can be null)
        probability: Float,
        prediction: String
    ): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val db = FirebaseFirestore.getInstance()

        // Tighter retry windows so Firebase SDK doesn’t keep retrying forever in the background
        FirebaseStorage.getInstance().apply {
            maxUploadRetryTimeMillis = TimeoutConfig.UPLOAD_MS
            maxOperationRetryTimeMillis = TimeoutConfig.UPLOAD_MS
            maxDownloadRetryTimeMillis = TimeoutConfig.URL_MS
        }

        // 1) Reserve Firestore doc id + "Scan N" label (you already have this repo)
        val (caseId, label) = ScanRepository.reserveScanLabelAndId(db, uid)


        val storage = FirebaseStorage.getInstance().reference
        val photoRef = storage.child("users/$uid/scans/$caseId.jpg")
        val heatmapRef = storage.child("users/$uid/scans/${caseId}_heatmap.jpg")

        // 3) Upload original photo
        val photoBytes = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
        }.toByteArray()
        val photoTask = photoRef.putBytes(photoBytes)
        val photoSnap = withTimeoutOrNull(TimeoutConfig.UPLOAD_MS) { photoTask.await() }
            ?: run {
                photoTask.cancel()
                return false
            }
        val photoUrl = withTimeoutOrNull(TimeoutConfig.URL_MS) {
            photoRef.downloadUrl.await().toString()
        } ?: return false

        // 4) Upload heatmap (if provided)
        var heatmapUrl: String? = null
        if (heatmap != null) {
            val heatmapBytes = ByteArrayOutputStream().apply {
                heatmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
            }.toByteArray()
            val heatTask = heatmapRef.putBytes(heatmapBytes)
            val heatSnap = withTimeoutOrNull(TimeoutConfig.UPLOAD_MS) { heatTask.await() }
                ?: run {
                    heatTask.cancel()
                    return false // or proceed without heatmap if you prefer
                }

            heatmapUrl = withTimeoutOrNull(TimeoutConfig.URL_MS) {
                heatmapRef.downloadUrl.await().toString()
            } ?: return false        }

        // 5) Firestore metadata
        val doc = hashMapOf(
            "user_id" to uid,
            "label" to label,
            "scan_url" to photoUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "timestamp_ms" to System.currentTimeMillis(),
            "prediction" to prediction,
            "probability" to probability.toDouble(),
            "status" to "completed",
            "heatmap_url" to heatmapUrl            // may be null if no heatmap
        )

        val writeOk = withTimeoutOrNull(TimeoutConfig.FIRESTORE_MS) {
            db.collection("lesion_case").document(caseId).set(doc).await()
            true
        } ?: false

        return writeOk
    }

    @Composable
    fun CameraPermissionGate(
        onGranted: @Composable () -> Unit,
        // optional UI if denied; keep it simple by default
        deniedContent: @Composable () -> Unit = {
            Text("Camera permission is required to continue.")
        }
    ) {
        val context = LocalContext.current
        val activity = context as? Activity
        var checked by remember { mutableStateOf(false) }
        var granted by remember { mutableStateOf(false) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            granted = isGranted
            checked = true
            if (!isGranted) {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                granted = true
                checked = true
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }

        // If user denied and checked is true, optionally show rationale + re-request button
        if (checked && !granted) {
            val shouldShowRationale =
                activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.CAMERA
                    )
                } == true

            Column {
                deniedContent()
                if (shouldShowRationale) {
                    Text("We use the camera to take/scan images. Please allow it to continue.")
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow Camera")
                    }
                } else {
                    // "Don’t ask again" or first hard denial → guide to Settings
                    Button(onClick = {
                        Toast.makeText(
                            context,
                            "Go to App Settings → Permissions → Camera",
                            Toast.LENGTH_LONG
                        ).show()
                    }) {
                        Text("Open Settings")
                    }
                }
            }
        } else if (granted) {
            onGranted()
        }
    }


private fun mapViewRectToImageSquare(
    viewLeft: Float,
    viewTop: Float,
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    previewViewWidth: Int,
    previewViewHeight: Int
): android.graphics.Rect {
    // Same scale on X & Y (cover) for PreviewView.ScaleType.FILL_CENTER
    val s = max(
        previewViewWidth.toFloat() / imageWidth.toFloat(),
        previewViewHeight.toFloat() / imageHeight.toFloat()
    )
    val scaledW = imageWidth * s
    val scaledH = imageHeight * s

    // Centered offsets inside the PreviewView
    val tx = (previewViewWidth - scaledW) / 2f
    val ty = (previewViewHeight - scaledH) / 2f

    // Map VIEW → IMAGE space
    val imgLeftF   = (viewLeft - tx) / s
    val imgTopF    = (viewTop  - ty) / s
    val imgRightF  = (viewLeft + viewWidth  - tx) / s
    val imgBottomF = (viewTop  + viewHeight - ty) / s

    // Enforce square: use the smaller side and center within the mapped rect
    val w = (imgRightF - imgLeftF)
    val h = (imgBottomF - imgTopF)
    val side = min(w, h)

    val cx = (imgLeftF + imgRightF) / 2f
    val cy = (imgTopF  + imgBottomF) / 2f

    val left   = (cx - side / 2f).roundToInt().coerceIn(0, imageWidth)
    val top    = (cy - side / 2f).roundToInt().coerceIn(0, imageHeight)
    val right  = (left + side.roundToInt()).coerceAtMost(imageWidth)
    val bottom = (top  + side.roundToInt()).coerceAtMost(imageHeight)

    // Final clamp to image bounds (keeps it square)
    val finalSide = min(right - left, bottom - top)
    return android.graphics.Rect(
        left,
        top,
        left + finalSide,
        top + finalSide
    )
}
private fun ImageProxy.toBitmapFast(): Bitmap {
    val yBuffer = planes[0].buffer       // Y
    val uBuffer = planes[1].buffer       // U
    val vBuffer = planes[2].buffer       // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // NV21
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        width,
        height,
        null
    )
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 75, out)
    val jpegBytes = out.toByteArray()
    val bmp = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

    val matrix = android.graphics.Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
}
private object TimeoutConfig {
    const val INFER_MS = 120_000L           // max time allowed for local model
    const val UPLOAD_MS = 15_000L         // max time per Storage upload
    const val URL_MS = 15_000L             // max time to get downloadUrl
    const val FIRESTORE_MS = 15_000L       // max time for Firestore write
}

// ---- Network guard (fast check) ----
private fun isOnline(ctx: Context): Boolean {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
            nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
