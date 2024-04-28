package net.craftventure.chat.bungee.util

object DurationUtils {
    fun ofSecondsToTicks(seconds: Double): Int = (seconds * 20).toInt()
}