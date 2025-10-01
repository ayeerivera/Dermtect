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
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch


// ---------- Small utilities ----------
fun nowTimestamp(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

// Focus box constants
val FOCUS_BOX_WIDTH = 280.dp
val FOCUS_BOX_HEIGHT = 340.dp

@Composable
fun TakePhotoScreen(
    onBackClick: () -> Unit = {}
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
    var flashEnabled by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val coroutineScope = rememberCoroutineScope()

    var hasSaved by remember { mutableStateOf(false) }

    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var squareRect by remember { mutableStateOf<Rect?>(null) }

    var isRunning by remember { mutableStateOf(false) }
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }
    var hasUploaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
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
                    .offset(y = (-80).dp)
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
                drawLine(Color.Cyan, Offset(right, top), Offset(right - lineLength, top), strokeWidth)
                drawLine(Color.Cyan, Offset(right, top), Offset(right, top + lineLength), strokeWidth)
                drawLine(Color.Cyan, Offset(left, bottom), Offset(left + lineLength, bottom), strokeWidth)
                drawLine(Color.Cyan, Offset(left, bottom), Offset(left, bottom - lineLength), strokeWidth)
                drawLine(Color.Cyan, Offset(right, bottom), Offset(right - lineLength, bottom), strokeWidth)
                drawLine(Color.Cyan, Offset(right, bottom), Offset(right, bottom - lineLength), strokeWidth)
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
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Take a photo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("3–6 (8–15 cm) inches away from the lesion", fontSize = 14.sp, color = Color.LightGray)
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFCDFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.width(75.dp))

                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .background(Color(0xFF0FB2B2), shape = CircleShape)
                        .clickable {
                            val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val fullBitmap = rotateBitmapAccordingToExif(context, file)
                                        squareRect?.let { rect ->
                                            val scaleX = fullBitmap.width.toFloat() / previewView.width.toFloat()
                                            val scaleY = fullBitmap.height.toFloat() / previewView.height.toFloat()

                                            val cropLeft = (rect.left * scaleX).toInt().coerceAtLeast(0)
                                            val cropTop = (rect.top * scaleY).toInt().coerceAtLeast(0)
                                            val cropWidth = (rect.width * scaleX).toInt()
                                                .coerceAtMost(fullBitmap.width - cropLeft)
                                            val cropHeight = (rect.height * scaleY).toInt()
                                                .coerceAtMost(fullBitmap.height - cropTop)

                                            capturedImage = Bitmap.createBitmap(
                                                fullBitmap, cropLeft, cropTop, cropWidth, cropHeight
                                            )
                                        } ?: run { capturedImage = fullBitmap }
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

                Spacer(Modifier.width(65.dp))
            }
        }

        // Show captured result / analysis
        capturedImage?.let { image ->
            // New image → reset and run inference
            LaunchedEffect(image) {
                hasUploaded = false
                if (!isRunning && inferenceResult == null) {
                    isRunning = true
                    val r = withContext(Dispatchers.Default) { tfService.infer(image) }
                    modelFlag = if (r.probability >= 0.0112f) "Malignant" else "Benign"
                    val merged = r.heatmap?.let { overlayBitmaps(image, it, 115) }
                    inferenceResult = r.copy(heatmap = merged)

                    isRunning = false
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

                        LaunchedEffect(capturedImage, r, hasUploaded) {
                            if (!hasUploaded && capturedImage != null) {
                                val ok = uploadScanWithLabel(
                                    bitmap = capturedImage!!,   // original cropped photo
                                    heatmap = r.heatmap,        //  merged overlay
                                    probability = r.probability,
                                    prediction = modelFlag
                                )
                                Log.d("Upload", if (ok) "Scan saved" else "Save failed")
                                hasUploaded = true
                            }
                        }



                        LesionCaseTemplate(
                            imageBitmap = image,
                            camBitmap = r.heatmap,
                            title = "Result",
                            timestamp = nowTimestamp(),
                            riskTitle = "Risk Assessment:",
                            riskDescription = riskCopy,
                            prediction = if (r.probability >= 0.0112f) "Malignant" else "Benign",
                            probability = r.probability,
                            onBackClick = { inferenceResult = null; capturedImage = null },

                            // NEW:
                            showPrimaryButtons = !hasSaved,      // show Save/Retake only before saving
                            showSecondaryActions = hasSaved,     // show "You can also…" only after saving
                            onSaveClick = {
                                // Call your existing upload function
                                coroutineScope.launch {
                                    val ok = uploadScanWithLabel(
                                        bitmap = capturedImage!!,
                                        heatmap = r.heatmap,
                                        probability = r.probability,
                                        prediction = if (r.probability >= 0.0112f) "Malignant" else "Benign"
                                    )
                                    if (ok) {
                                        Toast.makeText(context, "Scan saved", Toast.LENGTH_SHORT).show()
                                        hasSaved = true   // Switch UI: hide Save/Retake, show "You can also"
                                    } else {
                                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRetakeClick = {
                                inferenceResult = null
                                capturedImage = null
                            },
                            onDownloadClick = { /* PDF export */ },
                            onFindClinicClick = { /* open clinics screen */ }
                        )
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
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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

    // 1) Reserve Firestore doc id + "Scan N" label (you already have this repo)
    val (caseId, label) = ScanRepository.reserveScanLabelAndId(db, uid)

    // 2) Prepare Storage refs
    val storage = FirebaseStorage.getInstance().reference
    val photoRef = storage.child("users/$uid/scans/$caseId.jpg")
    val heatmapRef = storage.child("users/$uid/scans/${caseId}_heatmap.jpg")

    // 3) Upload original photo
    val photoBytes = ByteArrayOutputStream().apply {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
    }.toByteArray()
    photoRef.putBytes(photoBytes).await()
    val photoUrl = photoRef.downloadUrl.await().toString()

    // 4) Upload heatmap (if provided)
    var heatmapUrl: String? = null
    if (heatmap != null) {
        val heatmapBytes = ByteArrayOutputStream().apply {
            heatmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
        }.toByteArray()
        heatmapRef.putBytes(heatmapBytes).await()
        heatmapUrl = heatmapRef.downloadUrl.await().toString()
    }

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


    db.collection("lesion_case").document(caseId).set(doc).await()
    return true

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
            activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } == true

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
                    Toast.makeText(context, "Go to App Settings → Permissions → Camera", Toast.LENGTH_LONG).show()
                }) {
                    Text("Open Settings")
                }
            }
        }
    } else if (granted) {
        onGranted()
    }
}