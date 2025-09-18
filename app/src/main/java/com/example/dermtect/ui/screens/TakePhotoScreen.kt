package com.example.cameradermtect

import com.example.dermtect.ui.screens.ResultScreen
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import com.example.dermtect.R
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dermtect.ui.components.BackButton

import com.example.dermtect.tflt.TfLiteService
import com.example.dermtect.tflt.DermtectResult

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



// ✅ Constant for square size
val FOCUS_BOX_WIDTH = 280.dp
val FOCUS_BOX_HEIGHT = 340.dp // mas mahaba para skin lesion focus

@Composable
fun TakePhotoScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val tfService = remember { TfLiteService.get(context) }        // one instance
    val scope = rememberCoroutineScope()

    var lensFacing = CameraSelector.LENS_FACING_BACK
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

    // Save square region for cropping
    var squareRect by remember { mutableStateOf<Rect?>(null) }

    var isRunning by remember { mutableStateOf(false) }
    var inferenceResult by remember { mutableStateOf<DermtectResult?>(null) }

    DisposableEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1920))
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
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

        // Focus guide overlay (square + dimmed outside + corner lines)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-80).dp) // move square higher
            ) {
                // Dim background
                drawRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    size = size
                )

                // Use constants for dimensions
                val rectWidth = FOCUS_BOX_WIDTH.toPx()
                val rectHeight = FOCUS_BOX_HEIGHT.toPx()

                val left = (size.width - rectWidth) / 2
                val top = (size.height - rectHeight) / 2
                val right = left + rectWidth
                val bottom = top + rectHeight

                squareRect = Rect(left, top, right, bottom)

                // Transparent cut-out
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = GeometrySize(rectWidth, rectHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // Corner lines
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

        // Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 23.dp)
        ) {
            BackButton(
                modifier = Modifier
                    .align(Alignment.CenterStart),
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
            Text(
                text = "Take a photo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "3–6 (8–15 cm) inches away from the lesion",
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }

        // === BOTTOM CONTROLS (unchanged) ===
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

                Spacer(modifier = Modifier.width(75.dp))

                // Camera Button
                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .background(Color(0xFF0FB2B2), shape = CircleShape)
                        .clickable {
                            val file = File(
                                context.cacheDir,
                                "${System.currentTimeMillis()}.jpg"
                            )

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
                                            val cropWidth = (rect.width * scaleX).toInt().coerceAtMost(fullBitmap.width - cropLeft)
                                            val cropHeight = (rect.height * scaleY).toInt().coerceAtMost(fullBitmap.height - cropTop)

                                            capturedImage = Bitmap.createBitmap(fullBitmap, cropLeft, cropTop, cropWidth, cropHeight)
                                        } ?: run {
                                            capturedImage = fullBitmap
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                                        Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                                    }
                                }
                            )
                        }, contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.camera_vector),
                        contentDescription = "Capture",
                        modifier = Modifier
                            .size(24.dp)
                    )
                }


                Spacer(modifier = Modifier.width(65.dp))

            }
        }

        // === Show captured image popup (with buttons) ===
        capturedImage?.let { image ->

            // Auto-run inference when a new image is captured
            LaunchedEffect(image) {
                if (!isRunning && inferenceResult == null) {
                    isRunning = true
                    val r = withContext(Dispatchers.Default) { tfService.infer(image) }
                    inferenceResult = r
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
                        val prediction = if (r.isMalignant) "Malignant" else "Benign"

                        // ResultScreen is in ui.screens package; since TakePhotoScreen is also in that package,
                        // you can call it directly.
                        ResultScreen(
                            imageBitmap = image,
                            prediction = prediction,
                            probability = r.probability,
                            riskMessage = "Sample message: No serious risks detected. Please monitor for changes and consult a dermatologist if needed.",
                            camBitmap   = r.heatmap
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4DD0E1), // light cyan
                        Color(0xFF00796B)  // teal
                    )
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

fun rotateBitmapAccordingToExif(context: Context, file: File): Bitmap {
    val exif = ExifInterface(file.absolutePath)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    val rotationAngle = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    val bitmap = MediaStore.Images.Media.getBitmap(
        context.contentResolver,
        Uri.fromFile(file)
    )

    val matrix = Matrix().apply {
        postRotate(rotationAngle)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
