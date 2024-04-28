package net.craftventure.bukkit.ktx.util

import net.craftventure.bukkit.ktx.extension.colorFromHex
import net.craftventure.bukkit.ktx.extension.distanceTo
import org.bukkit.Color

// Mainly used for recolorable items (never really implemented on CV though)

object ColorNamer {
    private val colorNames = hashMapOf<String, String>()
        .apply {
            put("000000", "Black")
            // Removed around 1500 colors as I took this from internet somewhere
            put("FFFFFF", "White")
        }
        .map {
            val color = it.key
            val name = it.value
            ParsedColor(colorFromHex("#$color"), name)
        }

    fun nameForColor(color: Color) = colorNames.minByOrNull {
        color.distanceTo(it.color)
    }!!

    data class ParsedColor(
        val color: Color,
        val name: String,
        val red: Int = color.red,
        val green: Int = color.green,
        val blue: Int = color.blue
    )
}