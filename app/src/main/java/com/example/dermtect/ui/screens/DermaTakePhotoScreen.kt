package com.example.dermtect.ui.screens

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.dermtect.R
import com.example.dermtect.tflt.DermtectResult
import com.example.dermtect.tflt.TfLiteService
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.util.SkinGateConfig
import com.example.dermtect.util.SkinGateResult
import com.example.dermtect.util.analyzeImageForSkin
import com.example.dermtect.util.mapViewRectToImageSquare
import com.example.dermtect.util.overlayBitmaps
import com.example.dermtect.util.rotateBitmapAccordingToExif
import com.example.dermtect.util.toBitmapFast
import kotlinx.coroutines.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import kotlin.math.min
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.TimeoutCancellationException


/* ---------- constants (DERMA_*) ---------- */
private val DERMA_FOCUS_BOX = 320.dp
private val BottomBarHeight = 120.dp
private const val INFER_TIMEOUT_MS = 120_000L

@Composable
fun DermaTakePhotoScreen(
    onBackClick: () -> Unit = {},
) {
    // reuse the permission gate from your user screen file
    CameraPermissionGate(
        onGranted = { DermaTakePhotoScreenContent(onBackClick) },
        title = "Camera access is required",
        message = "We need the camera so you can capture a lesion photo."
    )
}

@Composable
private fun DermaTakePhotoScreenContent(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val tfService = remember { TfLiteService.get(context) }
    val cameraExecutor: ExecutorService = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) } // OFF/ON/AUTO

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var squareRect by remember { mutableStateOf<Rect?>(null) }

    var liveGateResult by remember { mutableStateOf<SkinGateResult?>(null) }
    var canCapture by remember { mutableStateOf(false) }

    // inference state
    var isRunning by remember { mutableStateOf(false) }
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }
    var modelFlag by remember { mutableStateOf("Benign") }
    var fullImagePage by remember { mutableStateOf<Int?>(null) }

    // ===== Camera bind (same pattern as user) =====
    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
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
                lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
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

    LaunchedEffect(flashMode) { imageCapture?.flashMode = flashMode }

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

        // Focus overlay (DERMA_FOCUS_BOX)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val rectWidth = DERMA_FOCUS_BOX.toPx()
                val rectHeight = DERMA_FOCUS_BOX.toPx()

                val left = (size.width - rectWidth) / 2
                val top = (size.height - rectHeight) / 2
                val right = left + rectWidth
                val bottom = top + rectHeight
                squareRect = Rect(left, top, right, bottom)

                val lineLength = 40.dp.toPx()
                val strokeWidth = 6f
                drawLine(Color.Cyan, Offset(left, top), Offset(left + lineLength, top), strokeWidth)
                drawLine(Color.Cyan, Offset(left, top), Offset(left, top + lineLength), strokeWidth)
                drawLine(Color.Cyan, Offset(right, top), Offset(right - lineLength, top), strokeWidth)
                drawLine(Color.Cyan, Offset(right, top), Offset(right, top + lineLength), strokeWidth)
                drawLine(Color.Cyan, Offset(left, bottom), Offset(left + lineLength, bottom), strokeWidth)
                drawLine(Color.Cyan, Offset(left, bottom), Offset(left, bottom - lineLength), strokeWidth)
                drawLine(Color.Cyan, Offset(right, bottom), Offset(right - lineLength, bottom), strokeWidth)
                drawLine(Color.Cyan, Offset(right, bottom), Offset(right, bottom - lineLength), strokeWidth)
            }
        }

        // Back + title
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
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Take a photo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("3–6 inches or 8–15 cm away from the lesion", fontSize = 14.sp, color = Color.LightGray)
        }

        // live gate status
        liveGateResult?.let { gate ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = BottomBarHeight + 24.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (gate.accepted) "Ready to capture" else gate.reason,
                    color = if (gate.accepted) Color(0xFF00C853) else Color(0xFFFF5252),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom controls (capture + flash)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFCDFFFF)),
            contentAlignment = Alignment.Center
        ) {
            val canShoot = canCapture
            val enabledColor = Color(0xFF0097A7)
            val disabledColor = Color(0xFFBDBDBD)

            // capture
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(65.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFBFFDFD), Color(0xFF88E7E7), Color(0xFF41A6A6))
                        ),
                        shape = CircleShape
                    )
                    .background(if (canShoot) enabledColor else disabledColor, CircleShape)
                    .clickable {
                        if (!canShoot) {
                            val msg = liveGateResult?.reason ?: "Not ready. Adjust framing/lighting and hold steady."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        val file = java.io.File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                        val output = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture?.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val fullBmp = rotateBitmapAccordingToExif(context, file)
                                    squareRect?.let { rect ->
                                        val crop = mapViewRectToImageSquare(
                                            viewLeft = rect.left,
                                            viewTop = rect.top,
                                            viewWidth = rect.width,
                                            viewHeight = rect.height,
                                            imageWidth = fullBmp.width,
                                            imageHeight = fullBmp.height,
                                            previewViewWidth = previewView.width,
                                            previewViewHeight = previewView.height
                                        )
                                        val side = min(crop.width(), crop.height()).coerceAtLeast(1)
                                        capturedImage = Bitmap.createBitmap(fullBmp, crop.left, crop.top, side, side)
                                    } ?: run {
                                        val side = min(fullBmp.width, fullBmp.height)
                                        val left = (fullBmp.width - side) / 2
                                        val top = (fullBmp.height - side) / 2
                                        capturedImage = Bitmap.createBitmap(fullBmp, left, top, side, side)
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.camera_vector),
                    contentDescription = "Capture",
                    modifier = Modifier.size(24.dp)
                )
            }

            // flash toggle
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .zIndex(5f)
                    .clickable {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                    ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                    else -> Icons.Filled.FlashAuto
                }
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = "Flash toggle",
                    tint = Color(0xFF0FB2B2),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(6.dp))
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

        /* ================== RESULTS (no saving) ================== */
        capturedImage?.let { image ->
            // run inference on new image
            LaunchedEffect(image) {
                inferenceResult = null
                modelFlag = "Benign"
                fullImagePage = null

                if (!isRunning) {
                    isRunning = true
                    try {
                        val r = withTimeout(INFER_TIMEOUT_MS) {
                            withContext(Dispatchers.Default) { tfService.infer(image) }
                        }
                        modelFlag = if (r.probability >= 0.0112f) "Malignant" else "Benign"
                        val merged = r.heatmap?.let { overlayBitmaps(image, it, 115) }
                        inferenceResult = r.copy(heatmap = merged)
                    } catch (_: TimeoutCancellationException) {
                        Toast.makeText(context, "Analysis took too long. Please retake.", Toast.LENGTH_LONG).show()
                        // keep the captured image so they can retake manually if desired
                    } catch (t: Throwable) {
                        Log.e("DermaTakePhoto", "Inference error", t)
                        Toast.makeText(context, "Failed to analyze image.", Toast.LENGTH_LONG).show()
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
                        val pPct = r.probability * 100f
                        val alerted = r.probability >= 0.0112f

                        // build possible conditions (same rule-set you used before)
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

                        // You already have this template in your project; it shows image + heatmap and a header.
                        LesionCaseTemplate(
                            imageBitmap = image,
                            camBitmap = r.heatmap,
                            title = "Result",
                            timestamp = nowTimestamp(),
                            riskTitle = "Prediction:",
                            riskDescription = modelFlag,     // just show Benign/Malignant here
                            prediction = modelFlag,
                            probability = r.probability,
                            showPrimaryButtons = false,
                            showSecondaryActions = false,
                            onImageClick = { page -> fullImagePage = page }, // 0=photo, 1=heatmap
                            isSaving = false,
                            onSaveClick = {},
                            onRetakeClick = {
                                inferenceResult = null
                                capturedImage = null
                                fullImagePage = null
                            },
                            onBackClick = {
                                inferenceResult = null
                                capturedImage = null
                                fullImagePage = null
                                onBackClick()
                            },
                            onDownloadClick = {},
                            onFindClinicClick = {},
                            compact = true
                        )

                        // Fullscreen viewer for photo/heatmap
                        if (fullImagePage != null) {
                            Dialog(onDismissRequest = { fullImagePage = null }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val displayBmp = when (fullImagePage) {
                                        0 -> image
                                        1 -> r.heatmap ?: image
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

/* ============== NOTE ==============
 * This file assumes the following already exist in your project (they do, from your user screen):
 *  - CameraPermissionGate(...) composable (import from your user screen’s package)
 *  - LesionCaseTemplate(...)
 *  - LesionIds.benignIds / lt30Ids / lt60Ids / lt80Ids / gte80Ids
 *  - util functions: rotateBitmapAccordingToExif, mapViewRectToImageSquare, toBitmapFast, overlayBitmaps
 *  - TfLiteService + DermtectResult
 * If any live under different packages, just adjust the imports above.
 * No saving, no Firestore, no PDFs here.
 */
