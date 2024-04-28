package net.craftventure.core.ktx.util

object TimeUtils {
    @JvmStatic
    fun secondsFromNow(seconds: Double): Long {
        return System.currentTimeMillis() + (1000 * seconds).toLong()
    }
}