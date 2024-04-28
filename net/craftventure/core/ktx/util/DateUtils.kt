package net.craftventure.core.ktx.util

import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.WeekFields
import java.util.*


object DateUtils {
    val weekFields: WeekFields = WeekFields.of(Locale.ENGLISH)

    val isWinter get() = true//LocalDateTime.now().let { it.isAfter(winter2021Start) && it.isBefore(winter2021End) }
    val isHalloween get() = false
    val isCoasterDay get() = LocalDateTime.now().matchesSafe(Month.AUGUST, 16)
    val isStarWarsDay get() = LocalDateTime.now().matchesSafe(Month.MAY, 4)
    val isAprilFools get() = LocalDateTime.now().matchesSafe(Month.APRIL, 1)
    val isEastern
        get() = LocalDateTime.now().matchesSafe(Month.APRIL, 9) ||
                LocalDateTime.now().matchesSafe(Month.APRIL, 10)

    fun LocalDateTime.matchesSafe(month: Month, dayOfMonth: Int) =
        matches(month, dayOfMonth) ||
                matches(month, dayOfMonth) ||
                plusHours(9).matches(month, dayOfMonth)

    fun LocalDateTime.matches(month: Month, dayOfMonth: Int) = this.month == month && this.dayOfMonth == dayOfMonth


    fun format(millis: Long, fallback: String, withSpaces: Boolean = true): String {
        if (millis < 0) return fallback
        if (millis == 0L) return "00s"
        var x = (millis / 1000).toInt()
        val seconds = x % 60
        x /= 60
        val minutes = x % 60
        x /= 60
        val hours = x % 24
        x /= 24
        val days = x

        var time = ""
        val space = if (withSpaces) " " else ""
        if (time.isNotEmpty() || days > 0)
            time = String.format("$time$space%02dd", days)
        if (time.isNotEmpty() || hours > 0)
            time = String.format("$time$space%02dh", hours)
        if (time.isNotEmpty() || minutes > 0)
            time = String.format("$time$space%02dm", minutes)
        if (time.isNotEmpty() || seconds > 0)
            time = String.format("$time$space%02ds", seconds)
        time = time.trim { it <= ' ' }
        return time.ifEmpty { fallback }
    }

    fun formatWithMillis(millis: Long, fallback: String, withSpaces: Boolean = true): String {
        var x = (millis / 1000).toInt()
        val seconds = x % 60
        x /= 60
        val minutes = x % 60
        x /= 60
        val hours = x % 24
        x /= 24
        val days = x

        var time = ""
        val space = if (withSpaces) " " else ""
        if (time.isNotEmpty() || days > 0)
            time = String.format("$time$space%02dd", days)
        if (time.isNotEmpty() || hours > 0)
            time = String.format("$time$space%02dh", hours)
        if (time.isNotEmpty() || minutes > 0)
            time = String.format("$time$space%02dm", minutes)
        if (time.isNotEmpty() || seconds > 0)
            time = String.format("$time$space%02ds", seconds)
        val millisLeft = millis % 1000L
        time = String.format("$time$space%02dms", millisLeft)
        time = time.trim { it <= ' ' }
        return if (time.isNotEmpty()) time else fallback
    }

    fun formatWithoutSeconds(millis: Long, fallback: String): String {
        var x = (millis / 1000).toInt()
        val seconds = x % 60
        x /= 60
        val minutes = x % 60
        x /= 60
        val hours = x % 24
        x /= 24
        val days = x

        var time = ""
        if (time.isNotEmpty() || days > 0)
            time = String.format("%s %02dd", time, days)
        if (time.isNotEmpty() || hours > 0)
            time = String.format("%s %02dh", time, hours)
        if (time.isNotEmpty() || minutes > 0)
            time = String.format("%s %02dm", time, minutes)
        time = time.trim { it <= ' ' }
        return if (time.isNotEmpty()) time else fallback
    }

    fun toString(calendar: Calendar): String {
        return String.format(
            "%02dh %02dm %02ds %s",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            calendar.timeZone.getDisplayName(calendar.timeZone.inDaylightTime(calendar.time), TimeZone.SHORT)
        )
    }
}
