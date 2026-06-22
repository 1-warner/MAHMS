package com.doodoobops.mahms.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/** Renders a simple text report to a single-page A4 PDF saved into Downloads. */
object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 @72dpi
    private const val PAGE_HEIGHT = 842

    /** Returns a human-readable destination on success, or null on failure. */
    fun export(context: Context, fileName: String, title: String, lines: List<String>): String? {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val header = Paint().apply { color = Color.rgb(0x1A, 0x66, 0xBE); textSize = 20f; isFakeBoldText = true }
        val sub = Paint().apply { color = Color.rgb(0x67, 0x74, 0x83); textSize = 11f }
        val body = Paint().apply { color = Color.rgb(0x23, 0x2A, 0x33); textSize = 12f }
        val footer = Paint().apply { color = Color.rgb(0x88, 0x93, 0xA0); textSize = 10f }
        val rule = Paint().apply { color = Color.rgb(0xE7, 0xEB, 0xEF); strokeWidth = 1f }

        var y = 60f
        canvas.drawText("Mpilo Central Hospital | MAHMS System", 40f, y, header)
        y += 18f
        canvas.drawText(title, 40f, y, sub)
        y += 12f
        canvas.drawLine(40f, y, (PAGE_WIDTH - 40).toFloat(), y, rule)
        y += 24f
        lines.forEach { line ->
            canvas.drawText(line, 40f, y, body)
            y += 18f
        }
        canvas.drawText("Confidential — generated ${DateUtils.prettyStamp(System.currentTimeMillis())}", 40f, (PAGE_HEIGHT - 40).toFloat(), footer)

        document.finishPage(page)

        return try {
            val safeName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os -> document.writeTo(os) }
                    "Downloads/$safeName"
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, safeName)
                FileOutputStream(file).use { os -> document.writeTo(os) }
                file.absolutePath
            }
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }
}
