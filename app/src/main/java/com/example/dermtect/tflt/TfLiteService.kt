package com.example.dermtect.tflt

import android.content.Context
import android.graphics.*
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DermtectResult(
    val probability: Float,       // P(malignant)
    val isMalignant: Boolean,     // probability >= tau
    val heatmap: Bitmap? = null,   // CAM overlay if model provides it
    val inputPreview: Bitmap? = null
)

class TfLiteService private constructor(
    ctx: Context,
    private val modelAssetPath: String = "dermtect_cam_selecttf.tflite",
    private val thresholdAssetPath: String = "rounded_threshold.json",
    private val expectsSize: Int = 224
) {

    companion object {
        @Volatile private var INSTANCE: TfLiteService? = null
        fun get(context: Context): TfLiteService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TfLiteService(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val interpreter: Interpreter
    private val tau: Float

    init {
        val appCtx = ctx.applicationContext

        // Load deployment threshold from assets (fallback to 0.0112 if missing)
        tau = runCatching {
            val txt = appCtx.assets.open(thresholdAssetPath).bufferedReader().use { it.readText() }
            // Accept either {"tau": 0.0112} or {"threshold_tau": 0.0112}
            JSONObject(txt).optDouble("tau", JSONObject(txt).optDouble("threshold_tau", 0.0112)).toFloat()
        }.getOrElse { 0.0112f }

        // Create TFLite interpreter
        val mapped = loadMappedFile(appCtx, modelAssetPath)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            // Add GPU delegate only if you include the dependency and want it
            // addDelegate(GpuDelegate())
        }
        interpreter = Interpreter(mapped, options)

        // Log tensor shapes once (helps when wiring outputs)
        Log.i("TfLite", "Inputs=${interpreter.inputTensorCount} Outputs=${interpreter.outputTensorCount}")
        repeat(interpreter.inputTensorCount) { i ->
            val t = interpreter.getInputTensor(i)
            Log.i("TfLite", "IN[$i] shape=${t.shape().contentToString()} dtype=${t.dataType()}")
        }
        repeat(interpreter.outputTensorCount) { i ->
            val t = interpreter.getOutputTensor(i)
            Log.i("TfLite", "OUT[$i] shape=${t.shape().contentToString()} dtype=${t.dataType()}")
        }
    }

    fun close() = runCatching { interpreter.close() }.onFailure { }.let {}

    /** Main entry point: returns probability + CAM overlay (if available). */
    fun infer(bitmap: Bitmap): DermtectResult {
        val (input, preview) = preprocessWithPreview(bitmap, expectsSize, expectsSize)

        return if (interpreter.outputTensorCount >= 2) {
            // Make the heatmap the same size as the preview (224x224) for perfect alignment
            inferDualOutput(input, bitmap.width, bitmap.height, preview)
        } else {
            inferSingleOutput(input, preview)
        }
    }

    // ---- Inference impls ----

    private fun inferSingleOutput(input: ByteBuffer, preview: Bitmap): DermtectResult {
        val probOut = FloatArray(1)
        interpreter.run(input, probOut)
        val p = probOut[0]
        return DermtectResult(
            probability = p,
            isMalignant = p >= tau,
            heatmap = null,
            inputPreview = preview
        )
    }

    /** Assumes OUT#0 = prob (1), OUT#1 = heatmap as (1, H, W, 1) float32. */
    private fun inferDualOutput(
        input: ByteBuffer,
        srcW: Int,
        srcH: Int,
        preview: Bitmap
    ): DermtectResult {
        // Inspect output shapes
        val s0 = interpreter.getOutputTensor(0).shape()
        val s1 = interpreter.getOutputTensor(1).shape()
        fun numel(s: IntArray) = s.fold(1) { a, b -> a * b }

        // Heuristic: larger tensor = heatmap, smaller = probability
        val (probIdx, camIdx) = if (numel(s0) <= numel(s1)) 0 to 1 else 1 to 0
        val probShape = interpreter.getOutputTensor(probIdx).shape()
        val camShape  = interpreter.getOutputTensor(camIdx).shape() // e.g., [1,H,W,1]

        // Prepare containers that match shapes exactly
        val probContainer: Any =
            if (probShape.size == 2) Array(probShape[0]) { FloatArray(probShape[1]) }
            else FloatArray(probShape[0]) // rarely [1]

        val camElems = numel(camShape)
        val camBuffer = ByteBuffer
            .allocateDirect(4 * camElems)
            .order(ByteOrder.nativeOrder())

        val outputs = hashMapOf<Int, Any>(
            probIdx to probContainer,
            camIdx  to camBuffer
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        // Read probability
        val p = when (probContainer) {
            is FloatArray -> probContainer[0]
            is Array<*>   -> (probContainer as Array<FloatArray>)[0][0]
            else          -> 0f
        }

        // Determine H and W from the heatmap shape
        val (H, W) = when (camShape.size) {
            4 -> camShape[1] to camShape[2]       // [1,H,W,1]
            3 -> camShape[1] to camShape[2]       // [H,W,1]
            2 -> camShape[0] to camShape[1]       // [H,W]
            else -> expectsSize to expectsSize
        }

        // Convert heatmap floats -> 2D array
        camBuffer.rewind()
        val cam = Array(H) { FloatArray(W) }
        for (y in 0 until H) {
            for (x in 0 until W) {
                cam[y][x] = camBuffer.float
            }
        }

        val heat = makeOverlayFromCam(cam, preview.width, preview.height)
        return DermtectResult(
            probability = p,
            isMalignant = p >= tau,
            heatmap = heat,
            inputPreview  = preview
        )
    }

    // ---- Pre/Post ----

    private fun preprocessWithPreview(src: Bitmap, dstW: Int, dstH: Int): Pair<ByteBuffer, Bitmap> {
        val resized = Bitmap.createScaledBitmap(src, dstW, dstH, true)

        val input = ByteBuffer.allocateDirect(4 * dstW * dstH * 3)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(dstW * dstH)
        resized.getPixels(pixels, 0, dstW, 0, 0, dstW, dstH)

        var i = 0
        for (y in 0 until dstH) {
            for (x in 0 until dstW) {
                val px = pixels[i++]
                input.putFloat(((px shr 16) and 0xFF).toFloat()) // R
                input.putFloat(((px shr 8) and 0xFF).toFloat())  // G
                input.putFloat((px and 0xFF).toFloat())          // B
            }
        }
        input.rewind()
        return input to resized
    }

    private fun makeOverlayFromCam(cam: Array<FloatArray>, outW: Int, outH: Int): Bitmap {
        // Normalize to [0,1]
        var mn = Float.POSITIVE_INFINITY
        var mx = Float.NEGATIVE_INFINITY
        for (y in cam.indices) for (x in cam[0].indices) {
            val v = cam[y][x]
            mn = min(mn, v)
            mx = max(mx, v)
        }
        val denom = if ((mx - mn) > 1e-9f) (mx - mn) else 1f
        val norm = Array(cam.size) { FloatArray(cam[0].size) }
        for (y in cam.indices) for (x in cam[0].indices) {
            norm[y][x] = ((cam[y][x] - mn) / denom).coerceIn(0f, 1f)
        }

        // Build 224x224 pseudo-JET
        val hm224 = Bitmap.createBitmap(cam[0].size, cam.size, Bitmap.Config.ARGB_8888)
        for (y in norm.indices) for (x in norm[0].indices) {
            hm224.setPixel(x, y, jet(norm[y][x]))
        }

        // Upscale and alpha-blend over source
        val heatUp = Bitmap.createScaledBitmap(hm224, outW, outH, true)
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (0.45f * 255).toInt() }
        canvas.drawBitmap(heatUp, 0f, 0f, paint)
        return out
    }

    private fun jet(v: Float): Int {
        fun clamp(x: Float) = when {
            x < 0f -> 0f
            x > 1f -> 1f
            else -> x
        }
        val r = (255f * clamp(1.5f - abs(4f * (v - 0.75f)))).toInt()
        val g = (255f * clamp(1.5f - abs(4f * (v - 0.50f)))).toInt()
        val b = (255f * clamp(1.5f - abs(4f * (v - 0.25f)))).toInt()
        return Color.argb(255, r, g, b)
    }

    private fun loadMappedFile(ctx: Context, assetPath: String): ByteBuffer {
        val afd = ctx.assets.openFd(assetPath)
        FileInputStream(afd.fileDescriptor).channel.use { ch ->
            return ch.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
        }
    }
}
