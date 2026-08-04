package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateAndOpenPdfReport(
        context: Context,
        reportTitle: String,
        batchName: String,
        groupName: String,
        dateRangeText: String,
        students: List<Student>,
        attendanceList: List<AttendanceRecord>
    ) {
        val pdfDocument = PdfDocument()

        // Standard A4 page size at 72 dpi: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()
        val textPaint = Paint()
        val borderPaint = Paint()

        // Background
        canvas.drawColor(Color.WHITE)

        // Title styling
        titlePaint.color = Color.parseColor("#0D0E15")
        titlePaint.textSize = 22f
        titlePaint.isFakeBoldText = true

        val subtitlePaint = Paint()
        subtitlePaint.color = Color.parseColor("#7B2CBF")
        subtitlePaint.textSize = 14f
        subtitlePaint.isFakeBoldText = true

        headerPaint.color = Color.parseColor("#141724")
        headerPaint.textSize = 11f
        headerPaint.isFakeBoldText = true

        textPaint.color = Color.parseColor("#1E2338")
        textPaint.textSize = 10f

        borderPaint.color = Color.parseColor("#CBD5E1")
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1f

        var y = 50f

        // Title Header
        canvas.drawText("RDA PHYSICAL ACADEMY", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("ATTENDANCE REPORT - $reportTitle", 40f, y, subtitlePaint)
        y += 25f

        // Divider Line
        paint.color = Color.parseColor("#9D4EDD")
        paint.strokeWidth = 2f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Report Meta Info
        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Batch: $batchName", 40f, y, headerPaint)
        canvas.drawText("Generated On: $currentDate", 320f, y, textPaint)
        y += 16f
        canvas.drawText("Group: $groupName", 40f, y, headerPaint)
        canvas.drawText("Range: $dateRangeText", 320f, y, textPaint)
        y += 30f

        // Table Header Rect
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRect(40f, y - 12f, 555f, y + 12f, paint)

        // Table Headers
        canvas.drawText("STUDENT NAME", 48f, y, headerPaint)
        canvas.drawText("MOBILE", 240f, y, headerPaint)
        canvas.drawText("PRESENT", 350f, y, headerPaint)
        canvas.drawText("ABSENT", 430f, y, headerPaint)
        canvas.drawText("ATTENDANCE %", 490f, y, headerPaint)
        y += 18f

        // Draw Table Rows
        var totalPresentAll = 0
        var totalAbsentAll = 0

        students.forEach { student ->
            if (y > 780f) return@forEach // Basic 1-page bounds check

            val studentAtt = attendanceList.filter { it.studentId == student.studentId || it.studentUid == student.uid }
            val presentCount = studentAtt.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = studentAtt.count { it.status == AttendanceStatus.ABSENT }
            val totalDays = studentAtt.size
            val percentage = if (totalDays > 0) (presentCount.toFloat() / totalDays * 100).toInt() else 100

            totalPresentAll += presentCount
            totalAbsentAll += absentCount

            canvas.drawLine(40f, y - 10f, 555f, y - 10f, borderPaint)

            canvas.drawText(student.name.take(22), 48f, y, textPaint)
            canvas.drawText(student.mobile, 240f, y, textPaint)
            canvas.drawText("$presentCount days", 350f, y, textPaint)
            canvas.drawText("$absentCount days", 430f, y, textPaint)
            canvas.drawText("$percentage%", 490f, y, headerPaint)

            y += 22f
        }

        canvas.drawLine(40f, y - 10f, 555f, y - 10f, paint)
        y += 15f

        // Footer Summary
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(40f, y - 12f, 555f, y + 25f, paint)

        canvas.drawText("TOTAL STUDENTS: ${students.size}", 48f, y + 5f, headerPaint)
        canvas.drawText("AGGREGATE PRESENT: $totalPresentAll", 240f, y + 5f, headerPaint)
        canvas.drawText("AGGREGATE ABSENT: $totalAbsentAll", 410f, y + 5f, headerPaint)

        // Footer Note
        canvas.drawText("Certified Official Document • RDA Physical Academy Management System", 140f, 810f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF File
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val pdfFile = File(reportsDir, "RDA_Attendance_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            // Open or Share PDF
            openPdfFile(context, pdfFile)
        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdfFile(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open Attendance PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF saved: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        }
    }
}
