package com.doodoobops.mahms.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Date/time helpers. App stores dates as "yyyy-MM-dd" and times as "HH:mm". */
object DateUtils {

    private val storeDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val displayStamp = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US)

    fun todayStore(): String = storeDate.format(Date())

    fun storeFormat(millis: Long): String = storeDate.format(Date(millis))

    fun parseStore(value: String): Long? = runCatching { storeDate.parse(value)?.time }.getOrNull()

    /** "yyyy-MM-dd" -> "dd MMM yyyy". Falls back to the raw value on parse failure. */
    fun prettyDate(store: String): String =
        runCatching { displayDate.format(storeDate.parse(store)!!) }.getOrDefault(store)

    /** Epoch millis -> "dd MMM yyyy · HH:mm". */
    fun prettyStamp(millis: Long): String = displayStamp.format(Date(millis))

    fun isFutureDate(store: String): Boolean {
        val t = parseStore(store) ?: return false
        return t >= startOfToday()
    }

    fun isPastDate(store: String): Boolean {
        val t = parseStore(store) ?: return false
        return t < startOfToday()
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "morning"
            h < 17 -> "afternoon"
            else -> "evening"
        }
    }

    fun currency(amount: Double): String = "USD %,.2f".format(Locale.US, amount)

    /** Last [days] store-format dates ending today, oldest first. */
    fun lastNDates(days: Int): List<String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        return (0 until days).map {
            storeDate.format(cal.time).also { cal.add(Calendar.DAY_OF_YEAR, 1) }
        }
    }
}
