package com.example.dermtect.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.dermtect.ui.components.DialogTemplate
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    data class CasePdfData(
        val title: String,
        val timestamp: String,
        val photo: Bitmap,
        val heatmap: Bitmap?,
        val shortMessage: String,
        val answers: List<Pair<String, String>>
    )

    fun createCasePdf(context: Context, data: CasePdfData): Uri {
        require(data.answers.isNotEmpty()) {
            "Questionnaire must be completed before exporting PDF."
        }

        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 24
        val lineGap = 15

        var pageNumber = 1
        fun newPage(): android.graphics.pdf.PdfDocument.Page {
            val info = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber++)
                .create()
            return pdf.startPage(info)
        }

        var page = newPage()
        var canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = margin.toFloat()

        // Header
        canvas.drawText(data.title, margin.toFloat(), y, titlePaint)
        y += titlePaint.textSize + lineGap
        canvas.drawText(data.timestamp, margin.toFloat(), y, textPaint)
        y += textPaint.textSize + (lineGap * 2)

        // Photo and heatmap
        fun drawScaledBitmap(bm: Bitmap, caption: String) {
            val availW = pageWidth - (margin * 2)
            val scale = availW.toFloat() / bm.width
            val targetH = (bm.height * scale).toInt()
            if (y + targetH + textPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page)
                page = newPage()
                canvas = page.canvas
                y = margin.toFloat()
            }
            val rect = Rect(margin, y.toInt(), margin + availW, y.toInt() + targetH)
            canvas.drawBitmap(bm, null, rect, paint)
            y += targetH + lineGap
            canvas.drawText(caption, margin.toFloat(), y, textPaint)
            y += textPaint.textSize + (lineGap * 2)
        }

        drawScaledBitmap(data.photo, "Original photo")
        data.heatmap?.let { drawScaledBitmap(it, "Heatmap overlay") }

        // Summary text
        fun drawWrappedText(label: String, body: String) {
            val labelPaint = Paint(textPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
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

            if (y + labelPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page)
                page = newPage()
                canvas = page.canvas
                y = margin.toFloat()
            }
            canvas.drawText(label, margin.toFloat(), y, labelPaint)
            y += labelPaint.textSize + lineGap

            val lines = breakLines(body, textPaint)
            lines.forEach { line ->
                if (y + textPaint.textSize + lineGap > pageHeight - margin) {
                    pdf.finishPage(page)
                    page = newPage()
                    canvas = page.canvas
                    y = margin.toFloat()
                }
                canvas.drawText(line, margin.toFloat(), y, textPaint)
                y += textPaint.textSize + lineGap
            }
            y += lineGap
        }

        drawWrappedText("Summary", data.shortMessage)

        // Answers
        fun drawSectionHeader(title: String) {
            val hdrPaint = Paint(titlePaint)
            if (y + hdrPaint.textSize + (lineGap * 2) > pageHeight - margin) {
                pdf.finishPage(page)
                page = newPage()
                canvas = page.canvas
                y = margin.toFloat()
            }
            canvas.drawText(title, margin.toFloat(), y, hdrPaint)
            y += hdrPaint.textSize + lineGap
        }

        drawSectionHeader("Assessment Report:")
        data.answers.forEach { (q, a) -> drawWrappedText("Q: $q", "A: $a") }

        pdf.finishPage(page)

        // Save
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
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            pdf.close()
            uri
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val folder = File(base, "Dermtect").apply { if (!exists()) mkdirs() }
            val file = File(folder, fileName)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

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
}

@Composable
fun CreateCasePdfWithDialog(
    context: Context,
    data: PdfExporter.CasePdfData,
    onPdfReady: (Uri) -> Unit,
    onNavigateToAssessment: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(data) {
        if (data.answers.isEmpty()) {
            showDialog = true
        } else {
            val uri = PdfExporter.createCasePdf(context, data)
            onPdfReady(uri)
        }
    }

    if (showDialog) {
        DialogTemplate(
            show = showDialog,
            title = "Questionnaire Required",
            description = "You must complete your questionnaire before exporting your report.",
            primaryText = "Go to Assessment Report",
            onPrimary = {
                onNavigateToAssessment()
                showDialog = false
            },
            tertiaryText = "Cancel",
            onTertiary = { showDialog = false },
            onDismiss = { showDialog = false },
            extraContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can tap the button above, or go manually to:\nSettings → Profile → My Assessment Report",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }
}
