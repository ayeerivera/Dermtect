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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    data class CasePdfData(
        val title: String,               // e.g., "Scan 3"
        val timestamp: String,           // formatted timestamp
        val photo: Bitmap,               // lesion photo
        val heatmap: Bitmap?,            // may be null
        val shortMessage: String,        // your therapeutic message
        val answers: List<Pair<String,String>> // questionnaire answers
    )

    fun createCasePdf(
        context: Context,
        data: CasePdfData
    ): Uri {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595  // A4 @ ~72dpi
        val pageHeight = 842
        val margin = 24
        val lineGap = 8

        fun newPage(): android.graphics.pdf.PdfDocument.Page {
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.pages.size + 1).create()
            return pdf.startPage(info)
        }

        var page = newPage()
        var canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = margin.toFloat()

        // Header
        canvas.drawText(data.title, margin.toFloat(), y, titlePaint)
        y += titlePaint.textSize + lineGap
        canvas.drawText(data.timestamp, margin.toFloat(), y, textPaint)
        y += textPaint.textSize + (lineGap * 2)

        // Helper to add a bitmap scaled to page width
        fun drawScaledBitmap(bm: Bitmap, caption: String) {
            val availW = pageWidth - (margin * 2)
            val scale = availW.toFloat() / bm.width
            val targetH = (bm.height * scale).toInt()
            // If about to overflow page, start a new one
            if (y + targetH + textPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page)
                page = newPage()
                canvas = page.canvas
                y = margin.toFloat()
            }
            val rect = Rect(margin, y.toInt(), margin + availW, y.toInt() + targetH)
            canvas.drawBitmap(bm, null, rect, paint)
            y += targetH + lineGap

            // caption
            canvas.drawText(caption, margin.toFloat(), y, textPaint)
            y += textPaint.textSize + (lineGap * 2)
        }

        // 1) Photo
        drawScaledBitmap(data.photo, "Original photo")

        // 2) Heatmap
        data.heatmap?.let { drawScaledBitmap(it, "Heatmap overlay") }

        // 3) Short message (wrap text)
        fun drawWrappedText(label: String, body: String) {
            val labelPaint = Paint(textPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val wrapWidth = (pageWidth - margin * 2).toFloat()
            fun breakLines(text: String, p: Paint): List<String> {
                val words = text.split(" ")
                val lines = mutableListOf<String>()
                var current = StringBuilder()
                for (w in words) {
                    val candidate = if (current.isEmpty()) w else current.toString() + " " + w
                    if (p.measureText(candidate) > wrapWidth) {
                        if (current.isNotEmpty()) lines.add(current.toString())
                        current = StringBuilder(w)
                    } else current = StringBuilder(candidate)
                }
                if (current.isNotEmpty()) lines.add(current.toString())
                return lines
            }

            // page break if needed
            if (y + labelPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page); page = newPage(); canvas = page.canvas; y = margin.toFloat()
            }
            canvas.drawText(label, margin.toFloat(), y, labelPaint)
            y += labelPaint.textSize + lineGap

            val lines = breakLines(body, textPaint)
            lines.forEach { line ->
                if (y + textPaint.textSize + lineGap > pageHeight - margin) {
                    pdf.finishPage(page); page = newPage(); canvas = page.canvas; y = margin.toFloat()
                }
                canvas.drawText(line, margin.toFloat(), y, textPaint)
                y += textPaint.textSize + lineGap
            }
            y += lineGap
        }

        drawWrappedText("Summary", data.shortMessage)

        // 4) Questionnaire answers
        fun drawSectionHeader(title: String) {
            val hdrPaint = Paint(titlePaint)
            if (y + hdrPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page); page = newPage(); canvas = page.canvas; y = margin.toFloat()
            }
            canvas.drawText(title, margin.toFloat(), y, hdrPaint)
            y += hdrPaint.textSize + lineGap
        }

        drawSectionHeader("Questionnaire")
        data.answers.forEach { (q, a) ->
            drawWrappedText("Q: $q", "A: $a")
        }

        pdf.finishPage(page)

        // ----- Save to Downloads and return Uri -----
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

            resolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            pdf.close()
            uri
        } else {
            // API 23–28
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(dir, "Dermtect").apply { if (!exists()) mkdirs() }
            val file = File(folder, fileName)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()

            // Return a content:// Uri via FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    fun openPdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}