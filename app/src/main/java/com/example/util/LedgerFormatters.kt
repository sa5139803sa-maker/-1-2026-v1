package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

object LedgerFormatters {
    private val arabicLocale = Locale("ar")
    private val decimalSymbols = DecimalFormatSymbols(Locale.US)
    private val wholeOrTwoDecimals = DecimalFormat("#,##0.##", decimalSymbols)
    private val exactTwoDecimals = DecimalFormat("#,##0.00", decimalSymbols)

    fun formatShekel(amount: Double, showSymbol: Boolean = true): String {
        val safe = if (abs(amount) < 0.005) 0.0 else amount
        val rounded = (safe * 100.0).roundToLong() / 100.0
        val formatted = if (abs(rounded - rounded.roundToLong()) < 0.005) {
            wholeOrTwoDecimals.format(rounded)
        } else {
            exactTwoDecimals.format(rounded)
        }
        return if (showSymbol) "\u200E$formatted ₪" else formatted
    }

    fun formatPercent(fraction: Float): String {
        val pct = (fraction * 100f).roundToLong().coerceIn(0, 100)
        return "$pct%"
    }

    fun formatTime(millis: Long): String {
        val fmt = SimpleDateFormat("hh:mm a", arabicLocale)
        return fmt.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val fmt = SimpleDateFormat("d MMM", arabicLocale)
        return fmt.format(Date(millis))
    }

    fun formatFullDate(millis: Long): String {
        val fmt = SimpleDateFormat("EEEE، d MMMM yyyy", arabicLocale)
        return fmt.format(Date(millis))
    }

    fun formatDayLabel(dayStartMillis: Long): String {
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        val yesterdayStart = addDays(todayStart, -1)
        return when {
            isSameDay(dayStartMillis, todayStart) -> "اليوم • ${formatShortDate(dayStartMillis)}"
            isSameDay(dayStartMillis, yesterdayStart) -> "أمس • ${formatShortDate(dayStartMillis)}"
            else -> {
                val fmt = SimpleDateFormat("EEE، d MMM", arabicLocale)
                fmt.format(Date(dayStartMillis))
            }
        }
    }

    fun formatDaysAgo(millis: Long): String {
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        val saleStart = startOfDayMillis(millis)
        val diffDays = TimeUnit.MILLISECONDS.toDays(todayStart - saleStart)
        return when {
            diffDays <= 0L -> "اليوم"
            diffDays == 1L -> "منذ يوم"
            diffDays == 2L -> "منذ يومين"
            diffDays in 3L..10L -> "منذ $diffDays أيام"
            else -> "منذ $diffDays يوماً"
        }
    }

    fun startOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    fun isSameDay(a: Long, b: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = a }
        val c2 = Calendar.getInstance().apply { timeInMillis = b }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun startOfMonthMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun combineDayWithCurrentTime(selectedDayStartMillis: Long): Long {
        val now = Calendar.getInstance()
        val todayStart = startOfDayMillis(now.timeInMillis)
        if (isSameDay(selectedDayStartMillis, todayStart)) {
            return now.timeInMillis
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDayStartMillis
            set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            set(Calendar.SECOND, now.get(Calendar.SECOND))
        }
        return cal.timeInMillis
    }

    private val arabicDiacriticsRegex = Regex("[\u064B-\u065F\u0670]")
    private val whitespaceRegex = Regex("\\s+")

    fun normalizeArabicText(text: String): String {
        return arabicDiacriticsRegex.replace(text.trim(), "")
            .lowercase(Locale.getDefault())
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(whitespaceRegex, " ")
            .trim()
    }

    fun matchesAllNameParts(target: String, query: String): Boolean {
        val normQuery = normalizeArabicText(query)
        if (normQuery.isEmpty()) return true
        val normTarget = normalizeArabicText(target)
        val tokens = normQuery.split(' ').filter { it.isNotEmpty() }
        return tokens.all { token -> normTarget.contains(token) }
    }
}
