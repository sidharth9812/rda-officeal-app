package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    fun getTodayString(daysOffset: Int = 0): String {
        val cal = Calendar.getInstance()
        if (daysOffset != 0) {
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun formatDate(year: Int, month: Int, dayOfMonth: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun isAfter(dateStr1: String, dateStr2: String): Boolean {
        if (dateStr1.isBlank() || dateStr2.isBlank()) return false
        return dateStr1 > dateStr2
    }

    fun isBefore(dateStr1: String, dateStr2: String): Boolean {
        if (dateStr1.isBlank() || dateStr2.isBlank()) return false
        return dateStr1 < dateStr2
    }
}
