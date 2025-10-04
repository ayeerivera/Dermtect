package com.example.dermtect.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

/** Tunables — tweak to your dataset / camera */
data class SkinGateConfig(
    val maxSide: Int = 220,                // downscale for speed
    val sampleStep: Int = 2,               // stride when sampling pixels
    val centerRadiusFactor: Float = 0.45f, // only evaluate this central circle
    val minSamples: Int = 1200,            // minimum usable samples

    // Lighting gates (HSV Value = brightness)
    val minBrightnessV: Float = 0.18f,     // reject if average V below
    val maxBrightnessV: Float = 0.92f,     // reject if average V above (flash/glare)

    // Blur gate (std-dev of luminance)
    val blurStdDevThreshold: Float = 12f,  // higher = stricter blur rejection

    // % of sampled pixels that should look like skin inside the center region
    val skinCoverageThreshold: Float = 0.28f
)

data class SkinGateResult(
    val accepted: Boolean,
    val reason: String,
    val skinCoverage: Float,
    val avgBrightnessV: Float,
    val luminanceStdDev: Float,
    val samplesUsed: Int
)

/** Main entry — combines center coverage + brightness + blur + skin heuristic */
fun analyzeImageForSkin(src: Bitmap, cfg: SkinGateConfig = SkinGateConfig()): SkinGateResult {
    val bm = src.downscale(cfg.maxSide)

    var skin = 0
    var total = 0

    var sumV = 0.0
    var sumY = 0.0
    var sumY2 = 0.0

    val w = bm.width
    val h = bm.height
    val cx = w / 2f
    val cy = h / 2f
    val r  = min(w, h) * cfg.centerRadiusFactor
    val r2 = r * r

    val hsv = FloatArray(3)

    for (y in 0 until h step cfg.sampleStep) {
        for (x in 0 until w step cfg.sampleStep) {
            val dx = x - cx
            val dy = y - cy
            // center mask
            if (dx*dx + dy*dy > r2) continue

            val c = bm.getPixel(x, y)
            if (Color.alpha(c) < 10) continue

            val rC = Color.red(c)
            val gC = Color.green(c)
            val bC = Color.blue(c)

            // brightness via HSV
            Color.RGBToHSV(rC, gC, bC, hsv)
            sumV += hsv[2]

            // luminance (BT.601)
            val yL = 0.299*rC + 0.587*gC + 0.114*bC
            sumY  += yL
            sumY2 += yL * yL

            if (isSkinPixel(rC, gC, bC, hsv)) skin++
            total++
        }
    }

    if (total < cfg.minSamples) {
        return SkinGateResult(
            accepted = false,
            reason = "Not enough usable center pixels ($total < ${cfg.minSamples}). Move closer / center the lesion.",
            skinCoverage = 0f,
            avgBrightnessV = 0f,
            luminanceStdDev = 0f,
            samplesUsed = total
        )
    }

    val coverage = skin.toFloat() / total
    val avgV = (sumV / total).toFloat()

    val meanY = (sumY / total).toFloat()
    val varY = ((sumY2 / total) - meanY*meanY).toFloat().coerceAtLeast(0f)
    val stdY = sqrt(varY)

    // 1) lighting gates
    if (avgV < cfg.minBrightnessV) {
        return SkinGateResult(false, "Too dark. Improve lighting.", coverage, avgV, stdY, total)
    }
    if (avgV > cfg.maxBrightnessV) {
        return SkinGateResult(false, "Too bright / overexposed. Reduce flash or glare.", coverage, avgV, stdY, total)
    }

    // 2) blur gate
    if (stdY < cfg.blurStdDevThreshold) {
        return SkinGateResult(false, "Too blurry. Hold steady and refocus.", coverage, avgV, stdY, total)
    }

    // 3) skin-coverage gate (center area only)
    if (coverage < cfg.skinCoverageThreshold) {
        return SkinGateResult(false, "Not enough skin in the center of the frame.", coverage, avgV, stdY, total)
    }

    return SkinGateResult(true, "OK", coverage, avgV, stdY, total)
}

/* ---------- Helpers ---------- */

private fun Bitmap.downscale(maxSide: Int): Bitmap {
    val scale = max(width, height).toFloat() / maxSide
    return if (scale > 1f) {
        Bitmap.createScaledBitmap(this, (width / scale).toInt(), (height / scale).toInt(), true)
    } else this
}

/** Heuristic “skin-like” pixel test using HSV + YCbCr */
private fun isSkinPixel(r: Int, g: Int, b: Int, hsv: FloatArray? = null): Boolean {
    val hsvArr = hsv ?: FloatArray(3).also { Color.RGBToHSV(r, g, b, it) }
    val h = hsvArr[0] // 0..360
    val s = hsvArr[1] // 0..1
    val v = hsvArr[2] // 0..1

    // HSV constraint: reddish/tan hues, moderate saturation, not too dark
    val hsvOk = (h in 0f..50f || h in 330f..360f) && s in 0.15f..0.68f && v > 0.25f

    // YCbCr (floating point version)
    val yf  =  0.299f*r + 0.587f*g + 0.114f*b
    val cbf = 128f - 0.168736f*r - 0.331264f*g + 0.5f*b
    val crf = 128f + 0.5f*r - 0.418688f*g - 0.081312f*b

    val ycrcbOk = (crf in 135f..180f) && (cbf in 85f..135f) && (yf in 20f..235f)

    return hsvOk && ycrcbOk
}
