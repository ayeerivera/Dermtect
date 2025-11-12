package com.example.dermtect.util

import android.content.Context
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun nowTimestamp(): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

fun overlayBitmaps(base: Bitmap, overlay: Bitmap, alpha: Int = 115): Bitmap {
    val config = base.config ?: Bitmap.Config.ARGB_8888
    val result = Bitmap.createBitmap(base.width, base.height, config)
    val canvas = Canvas(result)
    canvas.drawBitmap(base, 0f, 0f, null)
    val paint = Paint().apply { this.alpha = alpha }
    val scaled = if (overlay.width == base.width && overlay.height == base.height)
        overlay
    else
        Bitmap.createScaledBitmap(overlay, base.width, base.height, true)
    canvas.drawBitmap(scaled, 0f, 0f, paint)
    return result
}

fun rotateBitmapAccordingToExif(context: Context, file: java.io.File): Bitmap {
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

fun mapViewRectToImageSquare(
    viewLeft: Float,
    viewTop: Float,
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
    previewViewWidth: Int,
    previewViewHeight: Int
): android.graphics.Rect {
    val s = max(
        previewViewWidth.toFloat() / imageWidth.toFloat(),
        previewViewHeight.toFloat() / imageHeight.toFloat()
    )
    val scaledW = imageWidth * s
    val scaledH = imageHeight * s
    val tx = (previewViewWidth - scaledW) / 2f
    val ty = (previewViewHeight - scaledH) / 2f
    val imgLeftF = (viewLeft - tx) / s
    val imgTopF = (viewTop - ty) / s
    val imgRightF = (viewLeft + viewWidth - tx) / s
    val imgBottomF = (viewTop + viewHeight - ty) / s
    val w = (imgRightF - imgLeftF)
    val h = (imgBottomF - imgTopF)
    val side = min(w, h)
    val cx = (imgLeftF + imgRightF) / 2f
    val cy = (imgTopF + imgBottomF) / 2f
    val left = (cx - side / 2f).roundToInt().coerceIn(0, imageWidth)
    val top = (cy - side / 2f).roundToInt().coerceIn(0, imageHeight)
    val right = (left + side.roundToInt()).coerceAtMost(imageWidth)
    val bottom = (top + side.roundToInt()).coerceAtMost(imageHeight)
    val finalSide = min(right - left, bottom - top)
    return android.graphics.Rect(left, top, left + finalSide, top + finalSide)
}

fun ImageProxy.toBitmapFast(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 75, out)
    val jpegBytes = out.toByteArray()
    val bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
}

object TimeoutConfig {
    const val INFER_MS = 120_000L
    const val UPLOAD_MS = 15_000L
    const val URL_MS = 15_000L
    const val FIRESTORE_MS = 15_000L
}