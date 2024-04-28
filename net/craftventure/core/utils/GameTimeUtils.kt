package net.craftventure.core.utils

/**
 * This utility class is used for converting between the ingame time in ticks to ingame time as a friendly string. Note
 * that the time is INGAME.
 *
 *
 * http://www.minecraftwiki.net/wiki/Day/night_cycle
 *
 * @author Olof Larsson
 */
object GameTimeUtils {
    const val ticksAtMidnight = 18000
    const val ticksPerDay = 24000
    const val ticksPerHour = 1000
    const val ticksPerMinute = 1000.0 / 60.0
    const val ticksPerSecond = 1000.0 / 60.0 / 60.0

    fun hoursMinutesToTicks(hours: Int, minutes: Int): Long {
        var ret = ticksAtMidnight.toLong()
        ret += hours * ticksPerHour.toLong()
        ret += (minutes / 60.0 * ticksPerHour.toLong()).toLong()
        ret %= ticksPerDay.toLong()
        return ret
    }
}