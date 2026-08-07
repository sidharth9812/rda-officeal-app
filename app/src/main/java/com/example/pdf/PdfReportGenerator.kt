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

    /**
     * Generates a comprehensive or filtered group/batch attendance PDF report.
     */
    fun generateGroupAttendancePdf(
        context: Context,
        reportTitle: String,
        batchName: String,
        groupName: String,
        dateRangeText: String,
        students: List<Student>,
        attendanceList: List<AttendanceRecord>
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = 13f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            c.drawColor(Color.WHITE)
            // Top Accent Bar
            paint.color = Color.parseColor("#0284C7")
            paint.style = Paint.Style.FILL
            c.drawRect(0f, 0f, pageWidth.toFloat(), 8f, paint)

            // Header Title
            c.drawText("RDA PHYSICAL ACADEMY", 36f, 32f, titlePaint)
            c.drawText("ATTENDANCE REPORT — ${reportTitle.uppercase()}", 36f, 48f, subtitlePaint)

            // Accent Line
            paint.color = Color.parseColor("#0284C7")
            paint.strokeWidth = 1.5f
            c.drawLine(36f, 56f, (pageWidth - 36).toFloat(), 56f, paint)

            // Page Number Footer
            c.drawText("Page $pNum", (pageWidth - 70).toFloat(), 815f, textPaint)
            c.drawText("Official Document • RDA Physical Academy Management System", 36f, 815f, textPaint)
        }

        drawHeaderAndFooter(canvas, pageNumber)

        var y = 74f

        // Meta Box
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 42f, paint)
        canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 42f, borderPaint)

        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Batch: $batchName", 46f, y + 16f, headerPaint)
        canvas.drawText("Group: $groupName", 46f, y + 32f, headerPaint)

        canvas.drawText("Date Range: $dateRangeText", 280f, y + 16f, textPaint)
        canvas.drawText("Generated: $currentDate", 280f, y + 32f, textPaint)

        y += 56f

        // Table Header
        fun drawTableHeader(c: Canvas, startY: Float) {
            paint.color = Color.parseColor("#0F172A")
            paint.style = Paint.Style.FILL
            c.drawRect(36f, startY, (pageWidth - 36).toFloat(), startY + 22f, paint)

            val thPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
            }

            c.drawText("#", 44f, startY + 15f, thPaint)
            c.drawText("STUDENT ID", 68f, startY + 15f, thPaint)
            c.drawText("STUDENT NAME", 150f, startY + 15f, thPaint)
            c.drawText("MOBILE", 310f, startY + 15f, thPaint)
            c.drawText("PRESENT", 400f, startY + 15f, thPaint)
            c.drawText("ABSENT", 460f, startY + 15f, thPaint)
            c.drawText("ATT %", 518f, startY + 15f, thPaint)
        }

        drawTableHeader(canvas, y)
        y += 22f

        var totalPresentAll = 0
        var totalAbsentAll = 0

        students.forEachIndexed { index, student ->
            if (y > 760f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndFooter(canvas, pageNumber)
                y = 65f
                drawTableHeader(canvas, y)
                y += 22f
            }

            val studentAtt = attendanceList.filter { it.studentId == student.studentId || it.studentUid == student.uid }
            val presentCount = studentAtt.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = studentAtt.count { it.status == AttendanceStatus.ABSENT }
            val totalDays = studentAtt.size
            val percentage = if (totalDays > 0) (presentCount.toFloat() / totalDays * 100).toInt() else 0

            totalPresentAll += presentCount
            totalAbsentAll += absentCount

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 20f, paint)
            }

            canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 20f, borderPaint)

            canvas.drawText("${index + 1}", 44f, y + 14f, textPaint)
            canvas.drawText(student.studentId.ifBlank { "N/A" }, 68f, y + 14f, textPaint)
            canvas.drawText(student.name.take(24), 150f, y + 14f, headerPaint)
            canvas.drawText(student.mobile.ifBlank { "-" }, 310f, y + 14f, textPaint)

            val greenPaint = Paint(headerPaint).apply { color = Color.parseColor("#15803D") }
            val redPaint = Paint(headerPaint).apply { color = Color.parseColor("#B91C1C") }

            canvas.drawText("$presentCount d", 400f, y + 14f, greenPaint)
            canvas.drawText("$absentCount d", 460f, y + 14f, redPaint)

            val pctColor = when {
                percentage >= 80 -> "#15803D"
                percentage >= 60 -> "#B45309"
                else -> "#B91C1C"
            }
            val pctPaint = Paint(headerPaint).apply { color = Color.parseColor(pctColor) }
            canvas.drawText("$percentage%", 518f, y + 14f, pctPaint)

            y += 20f
        }

        // Summary Box at bottom
        if (y + 40f > 760f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeaderAndFooter(canvas, pageNumber)
            y = 65f
        }

        y += 10f
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 28f, paint)

        val summaryPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText("SUMMARY — TOTAL STUDENTS: ${students.size}", 48f, y + 18f, summaryPaint)
        canvas.drawText("TOTAL PRESENT MARKS: $totalPresentAll", 260f, y + 18f, summaryPaint)
        canvas.drawText("TOTAL ABSENT MARKS: $totalAbsentAll", 420f, y + 18f, summaryPaint)

        pdfDocument.finishPage(page)

        saveAndOpenPdf(context, pdfDocument, "RDA_Attendance_Report")
    }

    /**
     * Generates a detailed Individual Student Attendance PDF report.
     */
    fun generateIndividualStudentPdf(
        context: Context,
        student: Student,
        batchName: String,
        groupName: String,
        fromDate: String,
        toDate: String,
        attendanceList: List<AttendanceRecord>
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = 13f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            c.drawColor(Color.WHITE)
            paint.color = Color.parseColor("#0284C7")
            paint.style = Paint.Style.FILL
            c.drawRect(0f, 0f, pageWidth.toFloat(), 8f, paint)

            c.drawText("RDA PHYSICAL ACADEMY", 36f, 32f, titlePaint)
            c.drawText("INDIVIDUAL STUDENT ATTENDANCE REPORT", 36f, 48f, subtitlePaint)

            paint.color = Color.parseColor("#0284C7")
            paint.strokeWidth = 1.5f
            c.drawLine(36f, 56f, (pageWidth - 36).toFloat(), 56f, paint)

            c.drawText("Page $pNum", (pageWidth - 70).toFloat(), 815f, textPaint)
            c.drawText("Official Certified Document • RDA Physical Academy Management System", 36f, 815f, textPaint)
        }

        drawHeaderAndFooter(canvas, pageNumber)

        var y = 70f

        // Student Profile Header Box
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 80f, paint)
        canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 80f, borderPaint)

        val profileTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
        }

        canvas.drawText("Student Name: ${student.name.uppercase()}", 48f, y + 20f, profileTitlePaint)
        canvas.drawText("Student ID: ${student.studentId.ifBlank { "N/A" }}", 48f, y + 38f, headerPaint)
        canvas.drawText("Father's Name: ${student.fatherName.ifBlank { "N/A" }}", 48f, y + 54f, textPaint)
        canvas.drawText("Mobile: ${student.mobile.ifBlank { "N/A" }}", 48f, y + 70f, textPaint)

        canvas.drawText("Batch: $batchName", 310f, y + 20f, headerPaint)
        canvas.drawText("Group: $groupName", 310f, y + 38f, headerPaint)
        canvas.drawText("Target Exam: ${student.targetExam}", 310f, y + 54f, textPaint)
        canvas.drawText("Date Range: $fromDate to $toDate", 310f, y + 70f, textPaint)

        y += 94f

        // Attendance Stats Grid (4 Stat Boxes)
        val studentAtt = attendanceList
            .filter { it.studentId == student.studentId || it.studentUid == student.uid }
            .sortedBy { it.date }

        val presentCount = studentAtt.count { it.status == AttendanceStatus.PRESENT }
        val absentCount = studentAtt.count { it.status == AttendanceStatus.ABSENT }
        val totalDays = studentAtt.size
        val percentage = if (totalDays > 0) (presentCount.toFloat() / totalDays * 100).toInt() else 0

        val boxWidth = 122f
        val boxHeight = 40f
        val startX = 36f

        fun drawStatCard(x: Float, title: String, value: String, valueColor: String, bgHex: String) {
            paint.color = Color.parseColor(bgHex)
            paint.style = Paint.Style.FILL
            canvas.drawRect(x, y, x + boxWidth, y + boxHeight, paint)

            val bBorder = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(x, y, x + boxWidth, y + boxHeight, bBorder)

            val lblPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8f
                isFakeBoldText = true
            }
            val valPaint = Paint().apply {
                color = Color.parseColor(valueColor)
                textSize = 13f
                isFakeBoldText = true
            }

            canvas.drawText(title, x + 10f, y + 14f, lblPaint)
            canvas.drawText(value, x + 10f, y + 32f, valPaint)
        }

        drawStatCard(startX, "TOTAL DAYS", "$totalDays Days", "#0F172A", "#F1F5F9")
        drawStatCard(startX + 130f, "PRESENT DAYS", "$presentCount Days", "#15803D", "#DCFCE7")
        drawStatCard(startX + 260f, "ABSENT DAYS", "$absentCount Days", "#B91C1C", "#FEE2E2")
        drawStatCard(startX + 390f, "ATTENDANCE RATE", "$percentage%", "#0284C7", "#E0F2FE")

        y += 52f

        // Daily Logs Table Header
        fun drawDailyTableHeader(c: Canvas, startY: Float) {
            paint.color = Color.parseColor("#0F172A")
            paint.style = Paint.Style.FILL
            c.drawRect(36f, startY, (pageWidth - 36).toFloat(), startY + 22f, paint)

            val thPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
            }

            c.drawText("#", 46f, startY + 15f, thPaint)
            c.drawText("DATE", 80f, startY + 15f, thPaint)
            c.drawText("STATUS", 220f, startY + 15f, thPaint)
            c.drawText("MARKED BY ROLE", 340f, startY + 15f, thPaint)
        }

        drawDailyTableHeader(canvas, y)
        y += 22f

        if (studentAtt.isEmpty()) {
            canvas.drawText("No attendance records found for this student within the selected date range.", 48f, y + 20f, textPaint)
        } else {
            studentAtt.forEachIndexed { idx, record ->
                if (y > 760f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeaderAndFooter(canvas, pageNumber)
                    y = 65f
                    drawDailyTableHeader(canvas, y)
                    y += 22f
                }

                if (idx % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 20f, paint)
                }

                canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 20f, borderPaint)

                canvas.drawText("${idx + 1}", 46f, y + 14f, textPaint)
                canvas.drawText(record.date, 80f, y + 14f, headerPaint)

                val isPresent = record.status == AttendanceStatus.PRESENT
                val statusText = if (isPresent) "PRESENT" else "ABSENT"
                val statusColor = if (isPresent) "#15803D" else "#B91C1C"
                val stPaint = Paint(headerPaint).apply { color = Color.parseColor(statusColor) }

                canvas.drawText(statusText, 220f, y + 14f, stPaint)
                canvas.drawText(record.markedByRole.ifBlank { "ADMIN/LEADER" }, 340f, y + 14f, textPaint)

                y += 20f
            }
        }

        pdfDocument.finishPage(page)

        saveAndOpenPdf(context, pdfDocument, "RDA_Student_${student.studentId}_Report")
    }

    private fun saveAndOpenPdf(context: Context, pdfDocument: PdfDocument, filePrefix: String) {
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val pdfFile = File(reportsDir, "${filePrefix}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

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
