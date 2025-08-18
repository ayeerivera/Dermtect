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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.dermtect.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.input.pointer.pointerInput
import com.example.dermtect.ui.components.BackButton

@Composable
fun TakePhotoScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var lensFacing = CameraSelector.LENS_FACING_BACK
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

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

        // Bottom Controls
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

                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .background(Color(0xFF0FB2B2), shape = CircleShape)
                        .clickable {
                            val file = File(
                                context.cacheDir,
                                "${System.currentTimeMillis()}.jpg"
                            )

                            val outputOptions =
                                ImageCapture.OutputFileOptions.Builder(file).build()

                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturedImage =
                                            rotateBitmapAccordingToExif(context, file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(
                                            context,
                                            "Capture failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e(
                                            "CameraX",
                                            "Photo capture failed: ${exception.message}",
                                            exception
                                        )
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                            Image(
                                painter = painterResource(id = R.drawable.camera_vector),
                                contentDescription = "Capture",
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }

                Spacer(modifier = Modifier.width(45.dp))

                // Flash Toggle
                Image(
                    painter = painterResource(id = if (flashEnabled) R.drawable.flash_on else R.drawable.flash_off),
                    contentDescription = "Flash Toggle",
                    modifier = Modifier
                        .size(25.dp)
                        .clickable {
                            flashEnabled = !flashEnabled
                            imageCapture?.flashMode =
                                if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        }
                )
            }
        }

        // Show captured image popup
        capturedImage?.let { image ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { capturedImage = null },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Captured",
                        modifier = Modifier.size(300.dp)
                    )
                }
            }
        }
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
