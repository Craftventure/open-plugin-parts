package net.craftventure.bukkit.ktx.util

import net.craftventure.bukkit.ktx.extension.colorFromHex
import org.bukkit.Color
import kotlin.random.Random

object BukkitColorUtils {
    fun parseColor(color: String): Color {
        if (color.equals("random", ignoreCase = true)||color.equals("#random", ignoreCase = true)) {
            return Color.fromRGB(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
        } else {
            try {
                colorFromHex(color).let { return it }
            } catch (e: Exception) {
            }
            return try {
                Color.fromRGB(
                    Integer.valueOf(color.substring(1, 3), 16),
                    Integer.valueOf(color.substring(3, 5), 16),
                    Integer.valueOf(color.substring(5, 7), 16)
                )
            } catch (e: Exception) {
                val argb = Integer.valueOf(color)
                val r = argb and 0xFF
                val g = argb shr 8 and 0xFF
                val b = argb shr 16 and 0xFF
                val a = argb shr 24 and 0xFF
                Color.fromRGB(r, g, b)
            }
        }
    }
}