package net.craftventure.bukkit.ktx.util

import java.time.Duration

object TitleUtils {
    fun durationOfTicks(ticks: Int) = Duration.ofMillis(50L * ticks)
}