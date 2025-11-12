package com.example.dermtect.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.graphics.Color as GColor

// ---------- Await helper for Tasks ----------
private suspend fun <T> taskAwait(task: Task<T>): T =
    suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { res -> if (cont.isActive) cont.resume(res) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

// ---------- Age computation from "MM/dd/yyyy" ----------
private fun computeAgeFrom(birthday: String?): Int? {
    if (birthday.isNullOrBlank()) return null
    return try {
        val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
        sdf.isLenient = false
        val dob = sdf.parse(birthday) ?: return null

        val calDob = java.util.Calendar.getInstance().apply { time = dob }
        val calNow = java.util.Calendar.getInstance()

        var age = calNow.get(java.util.Calendar.YEAR) - calDob.get(java.util.Calendar.YEAR)
        val beforeBirthdayThisYear =
            calNow.get(java.util.Calendar.DAY_OF_YEAR) < calDob.get(java.util.Calendar.DAY_OF_YEAR)
        if (beforeBirthdayThisYear) age -= 1
        age
    } catch (_: Exception) {
        null
    }
}



object PdfExporter {

    data class CasePdfData(
        val userFullName: String = "",
        val birthday: String? = null,
        val reportId: String,
        val title: String,
        val timestamp: String,
        val photo: Bitmap,
        val heatmap: Bitmap?,
        val shortMessage: String,
        val possibleConditions: List<String> = emptyList(),
        val answers: List<Pair<String, String>>
    )
    // Keep short report IDs consistent
    fun shortReportFrom(caseId: String?, fallback: String = "TEMP0"): String =
        caseId?.takeLast(4)?.uppercase() ?: fallback


    // Extract "Possible identification(s): ..." from a paragraph.
    private fun extractPossibleFromSummary(raw: String): Pair<String, List<String>> {
        if (raw.isBlank()) return raw to emptyList()

        val re = Regex("(?i)\\bPossible\\s+Identifications?\\s*:\\s*(.+)")
        val match = re.find(raw)
        if (match == null) return raw to emptyList()

        val tail = match.groupValues[1]

        val items = tail.split(Regex("[•·•;]|\\s{2,}|,"))
            .map { it.trim().trim('"', '“', '”', '—', '-', '.', ' ') }
            .filter { it.isNotBlank() }
            .distinct()

        val cleanSummary = raw.substring(0, match.range.first).trim().trimEnd('.', ' ')
        return cleanSummary to items
    }



    // ---- Create clinic-style PDF (1–2 pages) ----
    fun createCasePdf(context: Context, data: CasePdfData): Uri {
        require(data.answers.isNotEmpty()) {
            "Questionnaire must be completed before exporting PDF."
        }

        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595    // A4 @ ~72dpi
        val pageHeight = 842
        val margin = 36
        val lineGap = 10
        val sectionGap = 16

        var pageNumber = 1
        fun newPage(): android.graphics.pdf.PdfDocument.Page {
            val info = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber++)
                .create()
            return pdf.startPage(info)
        }

        var page = newPage()
        var canvas = page.canvas

        val text12 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GColor.BLACK; textSize = 12f }
        val italic12 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GColor.BLACK; textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GColor.BLACK; textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bold12 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GColor.BLACK; textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lightRule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GColor.rgb(230, 230, 230) }

        var y = margin.toFloat()
        var pagesUsed = 1

        fun ensureSpace(h: Float): Boolean {
            if (y + h > pageHeight - margin) {
                if (pagesUsed >= 2) return false
                pdf.finishPage(page)
                page = newPage()
                canvas = page.canvas
                y = margin.toFloat()
                pagesUsed++
            }
            return true
        }

        fun wrap(text: String, p: Paint, maxW: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            for (w in words) {
                val cand = if (current.isEmpty()) w else current.toString() + " " + w
                if (p.measureText(cand) > maxW) {
                    if (current.isNotEmpty()) lines.add(current.toString())
                    current = StringBuilder(w)
                } else current = StringBuilder(cand)
            }
            if (current.isNotEmpty()) lines.add(current.toString())
            return lines
        }

        fun kv(k: String, v: String) {
            val key = "$k: "
            val keyW = bold12.measureText(key)
            if (!ensureSpace(text12.textSize + lineGap)) return
            canvas.drawText(key, margin.toFloat(), y, bold12)
            canvas.drawText(v, margin + keyW, y, text12)
            y += text12.textSize + lineGap
        }

        // ===== Title =====
        run {
            val title = "ANALYZED BY DERMTECT"
            if (!ensureSpace(titlePaint.textSize + sectionGap)) { /* unlikely */ }
            canvas.drawText(title, margin.toFloat(), y, titlePaint)
            y += titlePaint.textSize + 6f
            canvas.drawRect(margin.toFloat(), y, (pageWidth - margin).toFloat(), y + 1f, lightRule)
            y += sectionGap
        }

        // ===== Header fields =====
        run {
            val computedAge = computeAgeFrom(data.birthday)
            val agePart = computedAge?.let { " (Age: $it)" } ?: ""
            kv("Patient Name", data.userFullName.ifBlank { "__________________________" })
            kv("Report ID", data.reportId)
            kv("Birthday", (data.birthday ?: "").ifBlank { "__________" } + agePart)
            kv("Date Analyzed", data.timestamp)
            y += 4f // tighter to bring "Captured Images" up
        }

        // ===== Captured Images (two columns) =====
        run {
            canvas.drawText("Captured Images", margin.toFloat(), y, bold12)
            y += bold12.textSize + (lineGap - 4) // closer to images

            val availW = pageWidth - (margin * 2)
            val gutter = 10
            val colW = (availW - gutter) / 2
            val startY = y

            fun drawImageCol(bm: Bitmap?, colIndex: Int, caption: String): Int {
                val x = margin + colIndex * (colW + gutter)
                val maxH = 200
                return if (bm != null) {
                    val ratio = bm.width.toFloat() / bm.height.toFloat()
                    val tgtW = colW.toFloat()
                    val tgtH = (tgtW / ratio).toFloat().coerceAtMost(200f)
                    val r = Rect(x, y.toInt(), (x + tgtW).toInt(), (y + tgtH).toInt())
                    canvas.drawBitmap(bm, null, r, null)
                    canvas.drawText(caption, x.toFloat(), r.bottom + 10f, text12)
                    (tgtH + 10 + text12.textSize).toInt()
                } else {
                    val r = Rect(x, y.toInt(), x + colW, (y + 140).toInt())
                    canvas.drawRect(r, lightRule)
                    canvas.drawText("No image", x + 8f, r.exactCenterY(), text12)
                    canvas.drawText(caption, x.toFloat(), r.bottom + 10f, text12)
                    (140 + 10 + text12.textSize).toInt()
                }
            }

            val leftH = drawImageCol(data.photo, 0, "Original Image")
            val rightH = drawImageCol(data.heatmap, 1, "Processed / Analyzed Image")
            y = startY + maxOf(leftH, rightH) + (sectionGap - 2)
            ensureSpace(0f)
        }

        // ===== Summary (clean, no embedded identifications) =====
        val (cleanSummary, parsedPossible) = extractPossibleFromSummary(data.shortMessage)

        run {
            canvas.drawText("Summary", margin.toFloat(), y, bold12)
            y += bold12.textSize + lineGap

            val summaryText = cleanSummary.ifBlank { data.shortMessage }.trim()
            val quoted = if (summaryText.startsWith("\"") || summaryText.startsWith("“"))
                summaryText else "\"$summaryText\""
            val lines = wrap(quoted, italic12, (pageWidth - margin * 2).toFloat())
            for (ln in lines) {
                if (!ensureSpace(italic12.textSize + lineGap)) break
                canvas.drawText(ln, margin.toFloat(), y, italic12)
                y += italic12.textSize + lineGap
            }
            y += (sectionGap - lineGap)
        }

        // ===== Possible Identification (use model list; fallback to parsed) =====
        val possibleList = if (data.possibleConditions.isNotEmpty())
            data.possibleConditions else parsedPossible

        if (possibleList.isNotEmpty()) {
            canvas.drawText(
                "Possible Identification (1–${possibleList.size})",
                margin.toFloat(),
                y,
                bold12
            )
            y += bold12.textSize + lineGap

            possibleList.forEachIndexed { idx, item ->
                if (!ensureSpace(text12.textSize + lineGap)) return@forEachIndexed
                canvas.drawText("${idx + 1}. $item", margin.toFloat(), y, text12)
                y += text12.textSize + lineGap
            }
            y += (sectionGap - lineGap)
        }

        // ===== Questionnaire Answers (Q/A) =====
        if (data.answers.isNotEmpty()) {
            canvas.drawText("Questionnaire Answers", margin.toFloat(), y, bold12)
            y += bold12.textSize + lineGap

            val wrapWidth = (pageWidth - margin * 2).toFloat()
            data.answers.forEach { (q, a) ->
                if (!ensureSpace(text12.textSize * 2 + lineGap * 2)) return@forEach
                // Question
                wrap("Q: $q", bold12, wrapWidth).forEach { ln ->
                    if (!ensureSpace(text12.textSize + 2)) return@forEach
                    canvas.drawText(ln, margin.toFloat(), y, bold12); y += text12.textSize + 2
                }
                // Answer
                wrap("A: $a", text12, wrapWidth).forEach { ln ->
                    if (!ensureSpace(text12.textSize + lineGap)) return@forEach
                    canvas.drawText(ln, margin.toFloat(), y, text12); y += text12.textSize + lineGap
                }
            }
            y += (sectionGap - lineGap)
        }

        // ===== Disclaimer =====
        run {
            canvas.drawText("Disclaimer", margin.toFloat(), y, bold12)
            y += bold12.textSize + lineGap
            val d1 = "This report was generated by DermTect, a mobile-based skin analysis tool designed to assist with early detection and awareness."
            val d2 = "It does not replace professional medical consultation or diagnosis. Please consult a licensed dermatologist for further evaluation and treatment."
            for (ln in wrap(d1, italic12, (pageWidth - margin * 2).toFloat())) {
                if (!ensureSpace(italic12.textSize + lineGap)) break
                canvas.drawText(ln, margin.toFloat(), y, italic12); y += italic12.textSize + lineGap
            }
            for (ln in wrap(d2, italic12, (pageWidth - margin * 2).toFloat())) {
                if (!ensureSpace(italic12.textSize + lineGap)) break
                canvas.drawText(ln, margin.toFloat(), y, italic12); y += italic12.textSize + lineGap
            }
        }

        pdf.finishPage(page)

        // ===== Save =====
        val fileName = "Dermtect_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        return if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Dermtect")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Failed to create MediaStore record")
            resolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            pdf.close(); uri
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(base, "Dermtect").apply { if (!exists()) mkdirs() }
            val file = File(folder, fileName)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    // ---- Helper to open a PDF via chooser ----
    fun openPdf(context: Context, uri: Uri) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(view, "Open PDF"))
        } catch (e: Exception) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(share, "Open or share PDF"))
        }
    }

    // ---- Create locally, optionally upload to Storage, and write Firestore docs ----
    suspend fun saveReportAndPdf(
        context: Context,
        userId: String,
        baseData: PdfExporter.CasePdfData, // reportId already set
        uploadPdfToStorage: Boolean = true
    ): Uri {
        val db = FirebaseFirestore.getInstance()
        val storageRoot = FirebaseStorage.getInstance().reference
        val reportId = baseData.reportId

        // 1) Create PDF locally
        val localUri = PdfExporter.createCasePdf(context, baseData)

        // 2) (Optional) Upload to Firebase Storage
        var downloadUrl: String? = null
        var storagePath: String? = null
        if (uploadPdfToStorage) {
            val displayShort = baseData.reportId // this is the 4-char code shown in the PDF
            val safeName = "report_${displayShort}_${System.currentTimeMillis()}.pdf"
            val pdfRef = storageRoot.child("users/$userId/reports/$safeName")
            context.contentResolver.openInputStream(localUri)?.use { input ->
                taskAwait(pdfRef.putStream(input))
            }
            downloadUrl = taskAwait(pdfRef.downloadUrl).toString()
            storagePath = "users/$userId/reports/$safeName"
        }
        val ageVal = computeAgeFrom(baseData.birthday)

        // 3) Build Firestore payloads
        val fullPayload = mapOf(
            "reportId" to reportId,
            "userId" to userId,
            "userFullName" to baseData.userFullName,
            "birthday" to (baseData.birthday ?: ""),
            "age" to (ageVal ?: FieldValue.delete()),
            "title" to baseData.title,
            "summary" to baseData.shortMessage,
            "timestampText" to baseData.timestamp,
            "createdAt" to FieldValue.serverTimestamp(),
            "answers" to baseData.answers.map { mapOf("q" to it.first, "a" to it.second) },
            "hasHeatmap" to (baseData.heatmap != null),
            "possibleConditions" to baseData.possibleConditions,
            "fileUrl" to (downloadUrl ?: ""),
            "filePath" to (storagePath ?: "")
        )

        val userDoc = db.collection("users").document(userId)
            .collection("reports").document(reportId)

        val indexDoc = db.collection("reports_index")
            .document("${userId}__${reportId}")

        // 4) Write both in a batch
        taskAwait(
            db.runBatch { b ->
                b.set(userDoc, fullPayload)
                b.set(
                    indexDoc, mapOf(
                        "reportId" to reportId,
                        "userId" to userId,
                        "summary" to baseData.shortMessage,
                        "fileUrl" to (downloadUrl ?: ""),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        )

        return localUri
    }
}


